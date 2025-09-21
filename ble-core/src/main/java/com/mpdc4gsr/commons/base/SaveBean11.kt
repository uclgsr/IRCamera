package com.mpdc4gsr.commons.base

class SaveBean {
    var type: String? = null
    var mac: String? = null
    var name: String? = null

    constructor(type: String?, mac: String?, name: String?) {
        this.type = type
        this.mac = mac
        this.name = name
    }

    constructor()
}
