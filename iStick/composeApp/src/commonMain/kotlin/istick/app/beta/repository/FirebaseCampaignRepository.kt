// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/repository/FirebaseCampaignRepository.kt
package istick.app.beta.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Firebase implementation of the campaign repository
 */
class FirebaseCampaignRepository(
    private val authRepository: AuthRepository? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CampaignRepository {
    // Firebase Firestore instance
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val campaignsCollection = firestore.collection("campaigns")
    private val applicationsCollection = firestore.collection("applications")

    // State management
    private val _activeCampaigns = MutableStateFlow<List<Campaign>>(emptyList())
    override val activeCampaigns: StateFlow<List<Campaign>> = _activeCampaigns.asStateFlow()

    private val _userApplications = MutableStateFlow<List<CampaignApplication>>(emptyList())
    override val userApplications: StateFlow<List<CampaignApplication>> = _userApplications.asStateFlow()

    // Cache for campaigns
    private val campaignCache = mutableMapOf<String, Campaign>()

    // Cache for applications
    private val applicationCache = mutableMapOf<String, CampaignApplication>()

    // Flags for tracking initial loads
    private var activeCampaignsLoaded = false
    private var userApplicationsLoaded = false

    override fun observeActiveCampaigns(): Flow<List<Campaign>> {
        // If we haven't loaded active campaigns yet, do it now
        if (!activeCampaignsLoaded) {
            // Use a coroutine with the appropriate dispatcher
            kotlinx.coroutines.GlobalScope.launch(dispatcher) {
                try {
                    fetchActiveCampaigns()
                } catch (e: Exception) {
                    // Log error but don't crash
                    println("Error fetching active campaigns: ${e.message}")
                }
            }
        }

        return _activeCampaigns
    }

    override suspend fun fetchActiveCampaigns(): Result<List<Campaign>> = withContext(dispatcher) {
        try {
            // Query Firestore for active campaigns
            val querySnapshot = campaignsCollection
                .whereEqualTo("status", CampaignStatus.ACTIVE.name)
                .get()

            // Parse campaigns from documents
            val campaigns = querySnapshot.documents.mapNotNull { doc ->
                try {
                    parseCampaignDocument(doc)
                } catch (e: Exception) {
                    println("Error parsing campaign: ${e.message}")
                    null
                }
            }

            // Update cache and state
            campaigns.forEach { campaign ->
                campaignCache[campaign.id] = campaign
            }

            _activeCampaigns.value = campaigns
            activeCampaignsLoaded = true

            Result.success(campaigns)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to fetch active campaigns: ${e.message}", e))
        }
    }

    override suspend fun fetchCampaignDetails(campaignId: String): Result<Campaign> = withContext(dispatcher) {
        try {
            // Check cache first
            campaignCache[campaignId]?.let {
                return@withContext Result.success(it)
            }

            // Fetch from Firestore
            val campaignDoc = campaignsCollection.document(campaignId).get()
            if (!campaignDoc.exists) {
                return@withContext Result.failure(Exception("Campaign not found"))
            }

            // Parse campaign
            val campaign = parseCampaignDocument(campaignDoc)

            // Update cache
            campaignCache[campaignId] = campaign

            Result.success(campaign)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to fetch campaign details: ${e.message}", e))
        }
    }

    override suspend fun fetchUserApplications(userId: String): Result<List<CampaignApplication>> =
        withContext(dispatcher) {
            try {
                // Query Firestore for user's applications
                val querySnapshot = applicationsCollection
                    .whereEqualTo("carOwnerId", userId)
                    .get()

                // Parse applications
                val applications = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        parseApplicationDocument(doc)
                    } catch (e: Exception) {
                        println("Error parsing application: ${e.message}")
                        null
                    }
                }

                // Update cache and state
                applications.forEach { application ->
                    applicationCache[application.id] = application
                }

                _userApplications.value = applications
                userApplicationsLoaded = true

                Result.success(applications)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to fetch user applications: ${e.message}", e))
            }
        }

    override suspend fun applyCampaign(campaignId: String, carId: String): Result<CampaignApplication> =
        withContext(dispatcher) {
            try {
                // Get current user ID
                val userId = authRepository?.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                // Check if user already applied to this campaign
                val existingApplications = applicationsCollection
                    .whereEqualTo("campaignId", campaignId)
                    .whereEqualTo("carOwnerId", userId)
                    .whereEqualTo("carId", carId)
                    .get()

                if (existingApplications.documents.isNotEmpty()) {
                    return@withContext Result.failure(Exception("You have already applied to this campaign with this car"))
                }

                // Prepare application data
                val applicationData = mapOf(
                    "campaignId" to campaignId,
                    "carOwnerId" to userId,
                    "carId" to carId,
                    "status" to ApplicationStatus.PENDING.name,
                    "appliedAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "notes" to ""
                )

                // Add application to Firestore
                val applicationRef = applicationsCollection.add(applicationData)
                val applicationId = applicationRef.id

                // Create application object
                val application = CampaignApplication(
                    id = applicationId,
                    campaignId = campaignId,
                    carOwnerId = userId,
                    carId = carId,
                    status = ApplicationStatus.PENDING,
                    appliedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                // Update cache and state
                applicationCache[application.id] = application
                _userApplications.value = _userApplications.value + application

                // Add applicant to campaign's applicants list
                // Get current applicants
                val campaignDoc = campaignsCollection.document(campaignId).get()
                val currentApplicants = try {
                    campaignDoc.data()?.get("applicants") as? List<String> ?: emptyList()
                } catch (e: Exception) {
                    emptyList<String>()
                }

                // Add new applicant if not already present
                if (!currentApplicants.contains(userId)) {
                    val updatedApplicants = currentApplicants + userId
                    campaignsCollection.document(campaignId).update(
                        mapOf("applicants" to updatedApplicants)
                    )
                }

                Result.success(application)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to apply to campaign: ${e.message}", e))
            }
        }

    override suspend fun updateCampaignStatus(campaignId: String, status: CampaignStatus): Result<Campaign> =
        withContext(dispatcher) {
            try {
                // Check if campaign exists
                val campaignDoc = campaignsCollection.document(campaignId).get()
                if (!campaignDoc.exists) {
                    return@withContext Result.failure(Exception("Campaign not found"))
                }

                // Update status
                campaignsCollection.document(campaignId).update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )

                // Get updated campaign
                val updatedCampaignDoc = campaignsCollection.document(campaignId).get()
                val updatedCampaign = parseCampaignDocument(updatedCampaignDoc)

                // Update cache
                campaignCache[campaignId] = updatedCampaign

                // Update state if campaign is in active campaigns
                if (_activeCampaigns.value.any { it.id == campaignId }) {
                    if (status == CampaignStatus.ACTIVE) {
                        // If now active, update in list
                        _activeCampaigns.value = _activeCampaigns.value.map {
                            if (it.id == campaignId) updatedCampaign else it
                        }
                    } else {
                        // If no longer active, remove from list
                        _activeCampaigns.value = _activeCampaigns.value.filter { it.id != campaignId }
                    }
                } else if (status == CampaignStatus.ACTIVE) {
                    // If not in list but now active, add to list
                    _activeCampaigns.value = _activeCampaigns.value + updatedCampaign
                }

                Result.success(updatedCampaign)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to update campaign status: ${e.message}", e))
            }
        }

    /**
     * Create a new campaign
     */
    suspend fun createCampaign(campaign: Campaign): Result<Campaign> = withContext(dispatcher) {
        try {
            // Prepare campaign data
            val campaignData = mapOf(
                "brandId" to campaign.brandId,
                "title" to campaign.title,
                "description" to campaign.description,
                "stickerDetails" to mapOf(
                    "imageUrl" to campaign.stickerDetails.imageUrl,
                    "width" to campaign.stickerDetails.width,
                    "height" to campaign.stickerDetails.height,
                    "positions" to campaign.stickerDetails.positions.map { it.name },
                    "deliveryMethod" to campaign.stickerDetails.deliveryMethod.name
                ),
                "payment" to mapOf(
                    "amount" to campaign.payment.amount,
                    "currency" to campaign.payment.currency,
                    "paymentFrequency" to campaign.payment.paymentFrequency.name,
                    "paymentMethod" to campaign.payment.paymentMethod.name
                ),
                "requirements" to mapOf(
                    "minDailyDistance" to campaign.requirements.minDailyDistance,
                    "cities" to campaign.requirements.cities,
                    "carMakes" to campaign.requirements.carMakes,
                    "carModels" to campaign.requirements.carModels,
                    "carYearMin" to campaign.requirements.carYearMin,
                    "carYearMax" to campaign.requirements.carYearMax
                ),
                "status" to campaign.status.name,
                "startDate" to campaign.startDate,
                "endDate" to campaign.endDate,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "applicants" to emptyList<String>(),
                "approvedApplicants" to emptyList<String>()
            )

            // Add campaign to Firestore
            val campaignRef = campaignsCollection.add(campaignData)
            val campaignId = campaignRef.id

            // Create campaign object with ID
            val newCampaign = campaign.copy(id = campaignId)

            // Update cache
            campaignCache[newCampaign.id] = newCampaign

            // Update state if campaign is active
            if (newCampaign.status == CampaignStatus.ACTIVE) {
                _activeCampaigns.value = _activeCampaigns.value + newCampaign
            }

            Result.success(newCampaign)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to create campaign: ${e.message}", e))
        }
    }

    /**
     * Update an existing campaign
     */
    suspend fun updateCampaign(campaign: Campaign): Result<Campaign> = withContext(dispatcher) {
        try {
            // Prepare campaign data
            val campaignData = mapOf(
                "title" to campaign.title,
                "description" to campaign.description,
                "stickerDetails" to mapOf(
                    "imageUrl" to campaign.stickerDetails.imageUrl,
                    "width" to campaign.stickerDetails.width,
                    "height" to campaign.stickerDetails.height,
                    "positions" to campaign.stickerDetails.positions.map { it.name },
                    "deliveryMethod" to campaign.stickerDetails.deliveryMethod.name
                ),
                "payment" to mapOf(
                    "amount" to campaign.payment.amount,
                    "currency" to campaign.payment.currency,
                    "paymentFrequency" to campaign.payment.paymentFrequency.name,
                    "paymentMethod" to campaign.payment.paymentMethod.name
                ),
                "requirements" to mapOf(
                    "minDailyDistance" to campaign.requirements.minDailyDistance,
                    "cities" to campaign.requirements.cities,
                    "carMakes" to campaign.requirements.carMakes,
                    "carModels" to campaign.requirements.carModels,
                    "carYearMin" to campaign.requirements.carYearMin,
                    "carYearMax" to campaign.requirements.carYearMax
                ),
                "status" to campaign.status.name,
                "startDate" to campaign.startDate,
                "endDate" to campaign.endDate,
                "updatedAt" to System.currentTimeMillis()
            )

            // Update campaign in Firestore
            campaignsCollection.document(campaign.id).update(campaignData)

            // Update cache
            campaignCache[campaign.id] = campaign

            // Update state if campaign is in active campaigns
            if (_activeCampaigns.value.any { it.id == campaign.id }) {
                if (campaign.status == CampaignStatus.ACTIVE) {
                    // If still active, update in list
                    _activeCampaigns.value = _activeCampaigns.value.map {
                        if (it.id == campaign.id) campaign else it
                    }
                } else {
                    // If no longer active, remove from list
                    _activeCampaigns.value = _activeCampaigns.value.filter { it.id != campaign.id }
                }
            } else if (campaign.status == CampaignStatus.ACTIVE) {
                // If not in list but now active, add to list
                _activeCampaigns.value = _activeCampaigns.value + campaign
            }

            Result.success(campaign)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to update campaign: ${e.message}", e))
        }
    }

    /**
     * Get campaigns created by a brand
     */
    suspend fun getBrandCampaigns(brandId: String): Result<List<Campaign>> = withContext(dispatcher) {
        try {
            // Query Firestore for brand's campaigns
            val querySnapshot = campaignsCollection
                .whereEqualTo("brandId", brandId)
                .get()

            // Parse campaigns
            val campaigns = querySnapshot.documents.mapNotNull { doc ->
                try {
                    parseCampaignDocument(doc)
                } catch (e: Exception) {
                    println("Error parsing campaign: ${e.message}")
                    null
                }
            }

            // Update cache
            campaigns.forEach { campaign ->
                campaignCache[campaign.id] = campaign
            }

            Result.success(campaigns)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to get brand campaigns: ${e.message}", e))
        }
    }

    /**
     * Get campaigns matching user preferences
     */
    suspend fun getMatchingCampaigns(
        city: String,
        dailyDistance: Int,
        carMake: String,
        carModel: String,
        carYear: Int
    ): Result<List<Campaign>> = withContext(dispatcher) {
        try {
            // This query is complex and requires composite indexing in Firebase
            // For simplicity, we'll fetch all active campaigns and filter in memory
            val allCampaignsResult = fetchActiveCampaigns()
            val allCampaigns = allCampaignsResult.getOrNull() ?: emptyList()

            // Filter campaigns based on requirements
            val matchingCampaigns = allCampaigns.filter { campaign ->
                val req = campaign.requirements

                // Check daily distance requirement
                val meetsDistanceReq = dailyDistance >= req.minDailyDistance

                // Check city requirement (empty cities list means all cities are accepted)
                val meetsCityReq = req.cities.isEmpty() || req.cities.contains(city)

                // Check car make requirement (empty list means all makes are accepted)
                val meetsMakeReq = req.carMakes.isEmpty() || req.carMakes.contains(carMake)

                // Check car model requirement (empty list means all models are accepted)
                val meetsModelReq = req.carModels.isEmpty() || req.carModels.contains(carModel)

                // Check car year requirement
                val meetsYearMinReq = req.carYearMin == null || carYear >= req.carYearMin
                val meetsYearMaxReq = req.carYearMax == null || carYear <= req.carYearMax

                // Campaign matches if it meets all requirements
                meetsDistanceReq && meetsCityReq && meetsMakeReq &&
                        meetsModelReq && meetsYearMinReq && meetsYearMaxReq
            }

            Result.success(matchingCampaigns)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to get matching campaigns: ${e.message}", e))
        }
    }

    /**
     * Update application status
     */
    suspend fun updateApplicationStatus(
        applicationId: String,
        status: ApplicationStatus
    ): Result<CampaignApplication> = withContext(dispatcher) {
        try {
            // Fetch application
            val applicationDoc = applicationsCollection.document(applicationId).get()
            if (!applicationDoc.exists) {
                return@withContext Result.failure(Exception("Application not found"))
            }

            // Update status
            applicationsCollection.document(applicationId).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

            // If status is APPROVED, add to campaign's approved applicants
            if (status == ApplicationStatus.APPROVED) {
                val campaignId = try {
                    applicationDoc.data()?.get("campaignId") as? String ?: ""
                } catch (e: Exception) {
                    ""
                }

                val carOwnerId = try {
                    applicationDoc.data()?.get("carOwnerId") as? String ?: ""
                } catch (e: Exception) {
                    ""
                }

                if (campaignId.isNotEmpty() && carOwnerId.isNotEmpty()) {
                    // Get current approved applicants
                    val campaignDoc = campaignsCollection.document(campaignId).get()
                    val currentApprovedApplicants = try {
                        campaignDoc.data()?.get("approvedApplicants") as? List<String> ?: emptyList()
                    } catch (e: Exception) {
                        emptyList<String>()
                    }

                    // Add new approved applicant if not already present
                    if (!currentApprovedApplicants.contains(carOwnerId)) {
                        val updatedApprovedApplicants = currentApprovedApplicants + carOwnerId
                        campaignsCollection.document(campaignId).update(
                            mapOf("approvedApplicants" to updatedApprovedApplicants)
                        )
                    }
                }
            }

            // Parse updated application
            val updatedApplicationDoc = applicationsCollection.document(applicationId).get()
            val updatedApplication = parseApplicationDocument(updatedApplicationDoc)

            // Update cache
            applicationCache[applicationId] = updatedApplication

            // Update state if application is in user applications
            if (_userApplications.value.any { it.id == applicationId }) {
                _userApplications.value = _userApplications.value.map {
                    if (it.id == applicationId) updatedApplication else it
                }
            }

            Result.success(updatedApplication)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to update application status: ${e.message}", e))
        }
    }

    /**
     * Get applications for a campaign
     */
    suspend fun getCampaignApplications(campaignId: String): Result<List<CampaignApplication>> =
        withContext(dispatcher) {
            try {
                // Query Firestore for campaign applications
                val querySnapshot = applicationsCollection
                    .whereEqualTo("campaignId", campaignId)
                    .get()

                // Parse applications
                val applications = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        parseApplicationDocument(doc)
                    } catch (e: Exception) {
                        println("Error parsing application: ${e.message}")
                        null
                    }
                }

                // Update cache
                applications.forEach { application ->
                    applicationCache[application.id] = application
                }

                Result.success(applications)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to get campaign applications: ${e.message}", e))
            }
        }

    /**
     * Get recent campaigns
     */
    suspend fun getRecentCampaigns(limit: Int = 10): Result<List<Campaign>> = withContext(dispatcher) {
        try {
            // Query Firestore for recent active campaigns
            val querySnapshot = campaignsCollection
                .whereEqualTo("status", CampaignStatus.ACTIVE.name)
                .limit(limit.toLong())
                .get()

            // Parse campaigns
            val campaigns = querySnapshot.documents.mapNotNull { doc ->
                try {
                    parseCampaignDocument(doc)
                } catch (e: Exception) {
                    println("Error parsing campaign: ${e.message}")
                    null
                }
            }

            // Update cache
            campaigns.forEach { campaign ->
                campaignCache[campaign.id] = campaign
            }

            Result.success(campaigns)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to get recent campaigns: ${e.message}", e))
        }
    }

    /**
     * Clear cache and reset state
     */
    fun clearCache() {
        campaignCache.clear()
        applicationCache.clear()
        _activeCampaigns.value = emptyList()
        _userApplications.value = emptyList()
        activeCampaignsLoaded = false
        userApplicationsLoaded = false
    }

    /**
     * Parse a campaign document from Firestore
     */
    private fun parseCampaignDocument(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): Campaign {
        // Get basic fields
        val campaignId = doc.id
        val data = doc.data() ?: mapOf<String, Any>()

        // Use safe getters with proper type casting
        val brandId = data["brandId"] as? String ?: ""
        val title = data["title"] as? String ?: ""
        val description = data["description"] as? String ?: ""
        val statusString = data["status"] as? String ?: CampaignStatus.DRAFT.name
        val status = try {
            CampaignStatus.valueOf(statusString)
        } catch (e: Exception) {
            CampaignStatus.DRAFT
        }
        val startDate = data["startDate"] as? Long
        val endDate = data["endDate"] as? Long
        val createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        val updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()

        // Get nested sticker details
        val stickerDetailsMap = data["stickerDetails"] as? Map<*, *> ?: emptyMap<String, Any>()

        val stickerImageUrl = stickerDetailsMap["imageUrl"] as? String ?: ""
        val stickerWidth = (stickerDetailsMap["width"] as? Number)?.toInt() ?: 0
        val stickerHeight = (stickerDetailsMap["height"] as? Number)?.toInt() ?: 0

        // Parse positions enum list
        val positionStrings = (stickerDetailsMap["positions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val positions = positionStrings.mapNotNull { posStr ->
            try {
                StickerPosition.valueOf(posStr)
            } catch (e: Exception) {
                null
            }
        }

        // Parse delivery method enum
        val deliveryMethodString = (stickerDetailsMap["deliveryMethod"] as? String) ?: DeliveryMethod.CENTER.name
        val deliveryMethod = try {
            DeliveryMethod.valueOf(deliveryMethodString)
        } catch (e: Exception) {
            DeliveryMethod.CENTER
        }

        val stickerDetails = StickerDetails(
            imageUrl = stickerImageUrl,
            width = stickerWidth,
            height = stickerHeight,
            positions = positions,
            deliveryMethod = deliveryMethod
        )

        // Get nested payment details
        val paymentMap = data["payment"] as? Map<*, *> ?: emptyMap<String, Any>()

        val paymentAmount = (paymentMap["amount"] as? Number)?.toDouble() ?: 0.0
        val paymentCurrency = (paymentMap["currency"] as? String) ?: "RON"

        // Parse payment frequency enum
        val paymentFrequencyString = (paymentMap["paymentFrequency"] as? String) ?: PaymentFrequency.MONTHLY.name
        val paymentFrequency = try {
            PaymentFrequency.valueOf(paymentFrequencyString)
        } catch (e: Exception) {
            PaymentFrequency.MONTHLY
        }

        // Parse payment method enum
        val paymentMethodString = (paymentMap["paymentMethod"] as? String) ?: PaymentMethod.BANK_TRANSFER.name
        val paymentMethod = try {
            PaymentMethod.valueOf(paymentMethodString)
        } catch (e: Exception) {
            PaymentMethod.BANK_TRANSFER
        }

        val payment = PaymentDetails(
            amount = paymentAmount,
            currency = paymentCurrency,
            paymentFrequency = paymentFrequency,
            paymentMethod = paymentMethod
        )

        // Get nested requirements
        val requirementsMap = data["requirements"] as? Map<*, *> ?: emptyMap<String, Any>()

        val minDailyDistance = (requirementsMap["minDailyDistance"] as? Number)?.toInt() ?: 0
        val cities = (requirementsMap["cities"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val carMakes = (requirementsMap["carMakes"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val carModels = (requirementsMap["carModels"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val carYearMin = (requirementsMap["carYearMin"] as? Number)?.toInt()
        val carYearMax = (requirementsMap["carYearMax"] as? Number)?.toInt()

        val requirements = CampaignRequirements(
            minDailyDistance = minDailyDistance,
            cities = cities,
            carMakes = carMakes,
            carModels = carModels,
            carYearMin = carYearMin,
            carYearMax = carYearMax
        )

        // Get applicants and approved applicants
        val applicants = (data["applicants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val approvedApplicants = (data["approvedApplicants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        // Create Campaign object
        return Campaign(
            id = campaignId,
            brandId = brandId,
            title = title,
            description = description,
            stickerDetails = stickerDetails,
            payment = payment,
            requirements = requirements,
            status = status,
            startDate = startDate,
            endDate = endDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
            applicants = applicants,
            approvedApplicants = approvedApplicants
        )
    }

    /**
     * Parse an application document from Firestore
     */
    private fun parseApplicationDocument(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): CampaignApplication {
        val applicationId = doc.id
        val data = doc.data() ?: mapOf<String, Any>()

        val campaignId = data["campaignId"] as? String ?: ""
        val carOwnerId = data["carOwnerId"] as? String ?: ""
        val carId = data["carId"] as? String ?: ""
        val statusString = data["status"] as? String ?: ApplicationStatus.PENDING.name
        val status = try {
            ApplicationStatus.valueOf(statusString)
        } catch (e: Exception) {
            ApplicationStatus.PENDING
        }
        val appliedAt = data["appliedAt"] as? Long ?: System.currentTimeMillis()
        val updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()
        val notes = data["notes"] as? String ?: ""

        return CampaignApplication(
            id = applicationId,
            campaignId = campaignId,
            carOwnerId = carOwnerId,
            carId = carId,
            status = status,
            appliedAt = appliedAt,
            updatedAt = updatedAt,
            notes = notes
        )
    }
}