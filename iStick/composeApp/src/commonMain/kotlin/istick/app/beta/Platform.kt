package istick.app.beta

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform