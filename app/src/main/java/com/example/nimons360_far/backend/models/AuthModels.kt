package com.example.nimons360_far.backend.models

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val data: AuthData)

data class AuthData(val token: String, val expiresAt: String, val user: User)

data class User(val id: Int, val nim: String, val email: String, val fullName: String)
