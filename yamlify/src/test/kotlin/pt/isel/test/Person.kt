package pt.isel.test

import pt.isel.YamlArg

class Person (
    @YamlArg("city of birth") val from: String,
    @YamlArg("full name") val name: Name,
    val age : Int,
    val email : String?,
    @YamlArg("is_student") val student: Boolean,
    val addresses: List<Address> = emptyList()
)