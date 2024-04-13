package pt.isel.test

import pt.isel.YamlArg

class Name(
    @YamlArg("first name") val first: String,
    @YamlArg("last name") val last: String
)