package echo.shared

class ValidationException(message: String) : IllegalArgumentException(message)

class AuthenticationException(message: String) : IllegalArgumentException(message)

class AuthorizationException(message: String) : IllegalArgumentException(message)

class NotFoundException(message: String) : IllegalArgumentException(message)
