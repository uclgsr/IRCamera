package com.mpdc4gsr.commons.base.entity

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

class CheckableParcelable<T : Parcelable?> : CheckableItem<T?>, Parcelable {
    constructor()

    constructor(data: T?) : super(data)

    constructor(data: T?, isChecked: Boolean) : super(data, isChecked)

    protected constructor(`in`: Parcel) {
        val bundle = `in`.readBundle(javaClass.getClassLoader())
        if (bundle != null) {
            setData(bundle.getParcelable<Parcelable?>("items") as T?)
        }
        setChecked(`in`.readByte().toInt() != 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val bundle = Bundle()
        bundle.putParcelable("items", getData())
        dest.writeBundle(bundle)
        dest.writeByte(if (isChecked()) 1.toByte() else 0.toByte())
    }

    companion object {
        val CREATOR: Parcelable.Creator<CheckableParcelable<*>?> =
            object : Parcelable.Creator<CheckableParcelable<*>?> {
                override fun createFromParcel(source: Parcel): CheckableParcelable<*> {
                    return CheckableParcelable<Any?>(source)
                }

                override fun newArray(size: Int): Array<CheckableParcelable<*>?> {
                    return arrayOfNulls<CheckableParcelable<*>>(size)
                }
            }
    }
}
