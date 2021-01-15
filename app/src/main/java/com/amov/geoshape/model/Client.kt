package com.amov.geoshape.model

import java.io.Serializable

var count = 1

class Client : Serializable {
    var id = count++
    var lat: String = ""
    var long: String = ""
}

