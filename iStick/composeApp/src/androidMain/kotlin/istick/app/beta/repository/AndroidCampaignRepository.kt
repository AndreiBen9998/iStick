// Android-specific repository implementation with offline support
class AndroidCampaignRepository(
    private val campaignDao: CampaignDao
) : CampaignRepository {

    // Map entities to domain models
    private fun CampaignEntity.toDomain(): Campaign {
        return Campaign(
            id = id,
            brandId = brandId,
            title = title,
            description = description,
            status = CampaignStatus.valueOf(status),
            payment = PaymentDetails(
                amount = paymentAmount,
                currency = paymentCurrency
            ),
            // Map other fields
        )
    }

    // Map domain models to entities
    private fun Campaign.toEntity(): CampaignEntity {
        return CampaignEntity(
            id = id,
            brandId = brandId,
            title = title,
            description = description,
            status = status.name,
            paymentAmount = payment.amount,
            paymentCurrency = payment.currency
            // Map other fields
        )
    }

    override fun observeActiveCampaigns(): Flow<List<Campaign>> {
        return campaignDao.observeActiveCampaigns()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun fetchActiveCampaigns(): Result<List<Campaign>> {
        // Try to fetch from network
        try {
            // For now we'll use mock data
            val campaigns = getMockCampaigns()
            // Save to database
            campaignDao.clearAndInsert(campaigns.map { it.toEntity() })
            return Result.success(campaigns)
        } catch (e: Exception) {
            // Return what's in the database if network fails
            val cachedCampaigns = campaignDao.observeActiveCampaigns()
                .first()
                .map { it.toDomain() }

            return if (cachedCampaigns.isNotEmpty()) {
                Result.success(cachedCampaigns)
            } else {
                Result.failure(e)
            }
        }
    }

    // Implement other repository methods
}