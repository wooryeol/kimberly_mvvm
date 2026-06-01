package kr.co.kimberly.wma.network.model.login

data class LoginRequest(
    val agencyCd: String,
    val userId: String,
    val userPw: String,
    val mobileNo: String
)
