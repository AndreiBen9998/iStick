package istick.app.beta.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import istick.app.beta.auth.AuthRepository
import istick.app.beta.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

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
            kotlinx.coroutines.GlobalScope.launch(dispatcher) {
                try {
                    fetchActiveCampaigns()
                } catch (e: Exception) {
                    println("Error fetching active campaigns: ${e.message}")
                }
            }
        }
        return _activeCampaigns
    }

    override suspend fun fetchActiveCampaigns(): Result<List<Campaign>> = withContext(dispatcher) {
        try {
            // Fetch all campaigns
            val querySnapshot = campaignsCollection.get()
            val allCampaigns = mutableListOf<Campaign>()

            // Process each document
            for (document in querySnapshot.documents) {
                try {
                    val campaign = parseCampaignDocument(document)
                    if (campaign.status == CampaignStatus.ACTIVE) {
                        allCampaigns.add(campaign)
                        campaignCache[campaign.id] = campaign
                    }
                } catch (e: Exception) {
                    println("Error parsing campaign document: ${e.message}")
                }
            }

            _activeCampaigns.value = allCampaigns
            activeCampaignsLoaded = true

            Result.success(allCampaigns)
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
                // Fetch all applications
                val querySnapshot = applicationsCollection.get()
                val userApps = mutableListOf<CampaignApplication>()

                // Process each document
                for (document in querySnapshot.documents) {
                    try {
                        val application = parseApplicationDocument(document)
                        if (application.carOwnerId == userId) {
                            userApps.add(application)
                            applicationCache[application.id] = application
                        }
                    } catch (e: Exception) {
                        println("Error parsing application document: ${e.message}")
                    }
                }

                _userApplications.value = userApps
                userApplicationsLoaded = true

                Result.success(userApps)
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

                // Check if user already applied
                val existingApplications = _userApplications.value.any {
                    it.campaignId == campaignId &&
                            it.carOwnerId == userId &&
                            it.carId == carId
                }

                if (existingApplications) {
                    return@withContext Result.failure(Exception("You have already applied to this campaign with this car"))
                }

                // Prepare application data
                val applicationData = HashMap<String, Any>()
                applicationData["campaignId"] = campaignId
                applicationData["carOwnerId"] = userId
                applicationData["carId"] = carId
                applicationData["status"] = ApplicationStatus.PENDING.name
                applicationData["appliedAt"] = System.currentTimeMillis()
                applicationData["updatedAt"] = System.currentTimeMillis()
                applicationData["notes"] = ""

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
                updateCampaignApplicants(campaignId, userId)

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
                val updates = HashMap<String, Any>()
                updates["status"] = status.name
                updates["updatedAt"] = System.currentTimeMillis()

                campaignsCollection.document(campaignId).update(updates)

                // Get updated campaign
                val updatedCampaignDoc = campaignsCollection.document(campaignId).get()
                val updatedCampaign = parseCampaignDocument(updatedCampaignDoc)

                // Update cache
                campaignCache[campaignId] = updatedCampaign

                // Update active campaigns list if needed
                updateActiveCampaignsList(updatedCampaign)

                Result.success(updatedCampaign)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(Exception("Failed to update campaign status: ${e.message}", e))
            }
        }

    // Helper function to update campaign applicants
    private suspend fun updateCampaignApplicants(campaignId: String, userId: String) {
        try {
            val campaignDocRef = campaignsCollection.document(campaignId)
            val campaignDoc = campaignDocRef.get()

            if (campaignDoc.exists) {
                // Get current applicants
                val applicantsField = campaignDoc.data()?.get("applicants")
                val currentApplicants = mutableListOf<String>()

                if (applicantsField is List<*>) {
                    for (item in applicantsField) {
                        if (item is String) {
                            currentApplicants.add(item)
                        }
                    }
                }

                // Add new applicant if not already present
                if (!currentApplicants.contains(userId)) {
                    currentApplicants.add(userId)
                    val updates = HashMap<String, Any>()
                    updates["applicants"] = currentApplicants
                    campaignDocRef.update(updates)
                }
            }
        } catch (e: Exception) {
            println("Error updating campaign applicants: ${e.message}")
        }
    }

    // Helper function to update active campaigns list
    private fun updateActiveCampaignsList(campaign: Campaign) {
        val isInList = _activeCampaigns.value.any { it.id == campaign.id }

        if (campaign.status == CampaignStatus.ACTIVE) {
            if (isInList) {
                // Update existing campaign in list
                _activeCampaigns.value = _activeCampaigns.value.map {
                    if (it.id == campaign.id) campaign else it
                }
            } else {
                // Add to list if active
                _activeCampaigns.value = _activeCampaigns.value + campaign
            }
        } else if (isInList) {
            // Remove from list if not active
            _activeCampaigns.value = _activeCampaigns.value.filter { it.id != campaign.id }
        }
    }

    // Parse a campaign document
    private fun parseCampaignDocument(doc: DocumentSnapshot): Campaign {
        val campaignId = doc.id
        val data = doc.data() ?: emptyMap<String, Any>()

        // Extract basic fields with safe type casting
        val brandId = data["brandId"] as? String ?: ""
        val title = data["title"] as? String ?: ""
        val description = data["description"] as? String ?: ""
        val statusStr = data["status"] as? String ?: CampaignStatus.DRAFT.name
        val status = try {
            CampaignStatus.valueOf(statusStr)
        } catch (e: Exception) {
            CampaignStatus.DRAFT
        }

        val startDate = data["startDate"] as? Long
        val endDate = data["endDate"] as? Long
        val createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        val updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()

        // Parse applicants lists
        val applicants = parseStringList(data["applicants"])
        val approvedApplicants = parseStringList(data["approvedApplicants"])

        // Parse nested objects
        val stickerDetails = parseStickerDetails(data["stickerDetails"])
        val payment = parsePaymentDetails(data["payment"])
        val requirements = parseCampaignRequirements(data["requirements"])

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

    // Parse an application document
    private fun parseApplicationDocument(doc: DocumentSnapshot): CampaignApplication {
        val applicationId = doc.id
        val data = doc.data() ?: emptyMap<String, Any>()

        val campaignId = data["campaignId"] as? String ?: ""
        val carOwnerId = data["carOwnerId"] as? String ?: ""
        val carId = data["carId"] as? String ?: ""

        val statusStr = data["status"] as? String ?: ApplicationStatus.PENDING.name
        val status = try {
            ApplicationStatus.valueOf(statusStr)
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

    // Helper functions for parsing nested objects
    private fun parseStickerDetails(value: Any?): StickerDetails {
        if (value !is Map<*, *>) return StickerDetails()

        val imageUrl = value["imageUrl"] as? String ?: ""
        val width = (value["width"] as? Number)?.toInt() ?: 0
        val height = (value["height"] as? Number)?.toInt() ?: 0

        // Parse positions enum values
        val positions = mutableListOf<StickerPosition>()
        val positionsRaw = value["positions"]
        if (positionsRaw is List<*>) {
            for (pos in positionsRaw) {
                if (pos is String) {
                    try {
                        positions.add(StickerPosition.valueOf(pos))
                    } catch (e: Exception) {
                        // Skip invalid positions
                    }
                }
            }
        }

        // Parse delivery method enum
        val deliveryMethodStr = value["deliveryMethod"] as? String ?: DeliveryMethod.CENTER.name
        val deliveryMethod = try {
            DeliveryMethod.valueOf(deliveryMethodStr)
        } catch (e: Exception) {
            DeliveryMethod.CENTER
        }

        return StickerDetails(
            imageUrl = imageUrl,
            width = width,
            height = height,
            positions = positions,
            deliveryMethod = deliveryMethod
        )
    }

    private fun parsePaymentDetails(value: Any?): PaymentDetails {
        if (value !is Map<*, *>) return PaymentDetails()

        val amount = (value["amount"] as? Number)?.toDouble() ?: 0.0
        val currency = value["currency"] as? String ?: "RON"

        // Parse payment frequency enum
        val frequencyStr = value["paymentFrequency"] as? String ?: PaymentFrequency.MONTHLY.name
        val frequency = try {
            PaymentFrequency.valueOf(frequencyStr)
        } catch (e: Exception) {
            PaymentFrequency.MONTHLY
        }

        // Parse payment method enum
        val methodStr = value["paymentMethod"] as? String ?: PaymentMethod.BANK_TRANSFER.name
        val method = try {
            PaymentMethod.valueOf(methodStr)
        } catch (e: Exception) {
            PaymentMethod.BANK_TRANSFER
        }

        return PaymentDetails(
            amount = amount,
            currency = currency,
            paymentFrequency = frequency,
            paymentMethod = method
        )
    }

    private fun parseCampaignRequirements(value: Any?): CampaignRequirements {
        if (value !is Map<*, *>) return CampaignRequirements()

        val minDailyDistance = (value["minDailyDistance"] as? Number)?.toInt() ?: 0
        val cities = parseStringList(value["cities"])
        val carMakes = parseStringList(value["carMakes"])
        val carModels = parseStringList(value["carModels"])
        val carYearMin = (value["carYearMin"] as? Number)?.toInt()
        val carYearMax = (value["carYearMax"] as? Number)?.toInt()

        return CampaignRequirements(
            minDailyDistance = minDailyDistance,
            cities = cities,
            carMakes = carMakes,
            carModels = carModels,
            carYearMin = carYearMin,
            carYearMax = carYearMax
        )
    }

    // Helper function to parse string lists
    private fun parseStringList(value: Any?): List<String> {
        val result = mutableListOf<String>()

        if (value is List<*>) {
            for (item in value) {
                if (item is String) {
                    result.add(item)
                }
            }
        }

        return result
    }

    // Additional methods that might be needed

    /**
     * Create a new campaign
     */
    suspend fun createCampaign(campaign: Campaign): Result<Campaign> = withContext(dispatcher) {
        try {
            // Prepare campaign data map
            val campaignData = HashMap<String, Any>()
            campaignData["brandId"] = campaign.brandId
            campaignData["title"] = campaign.title
            campaignData["description"] = campaign.description
            campaignData["status"] = campaign.status.name

            // Add dates
            campaign.startDate?.let { campaignData["startDate"] = it }
            campaign.endDate?.let { campaignData["endDate"] = it }

            campaignData["createdAt"] = System.currentTimeMillis()
            campaignData["updatedAt"] = System.currentTimeMillis()

            // Add nested objects
            campaignData["stickerDetails"] = mapOf(
                "imageUrl" to campaign.stickerDetails.imageUrl,
                "width" to campaign.stickerDetails.width,
                "height" to campaign.stickerDetails.height,
                "positions" to campaign.stickerDetails.positions.map { it.name },
                "deliveryMethod" to campaign.stickerDetails.deliveryMethod.name
            )

            campaignData["payment"] = mapOf(
                "amount" to campaign.payment.amount,
                "currency" to campaign.payment.currency,
                "paymentFrequency" to campaign.payment.paymentFrequency.name,
                "paymentMethod" to campaign.payment.paymentMethod.name
            )

            campaignData["requirements"] = mapOf(
                "minDailyDistance" to campaign.requirements.minDailyDistance,
                "cities" to campaign.requirements.cities,
                "carMakes" to campaign.requirements.carMakes,
                "carModels" to campaign.requirements.carModels,
                "carYearMin" to campaign.requirements.carYearMin,
                "carYearMax" to campaign.requirements.carYearMax
            )

            campaignData["applicants"] = emptyList<String>()
            campaignData["approvedApplicants"] = emptyList<String>()

            // Add to Firestore
            val campaignRef = campaignsCollection.add(campaignData)
            val campaignId = campaignRef.id

            // Create new campaign with ID
            val newCampaign = campaign.copy(id = campaignId)

            // Update cache
            campaignCache[campaignId] = newCampaign

            // Update active campaigns list if needed
            if (newCampaign.status == CampaignStatus.ACTIVE) {
                _activeCampaigns.value = _activeCampaigns.value + newCampaign
            }

            Result.success(newCampaign)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Failed to create campaign: ${e.message}", e))
        }
    }

    // Add any other methods you need here
}