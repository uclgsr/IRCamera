package com.mpdc4gsr.ble

import com.mpdc4gsr.ble.util.Logger
import com.mpdc4gsr.commons.observer.Observable
import com.mpdc4gsr.commons.poster.ThreadMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EasyBLEBuilder internal constructor() {
    var bondController: BondController? = null
    var deviceCreator: DeviceCreator? = null
    var methodDefaultThreadMode: ThreadMode = ThreadMode.MAIN
    var executorService: ExecutorService? = DEFAULT_EXECUTOR_SERVICE
    var scanConfiguration: ScanConfiguration? = null
    var observable: Observable? = null
    var logger: Logger? = null
    var isObserveAnnotationRequired: Boolean = false
    var scannerType: ScannerType? = null
    var useNordicBleBackend: Boolean = true

    fun setScannerType(scannerType: ScannerType?): EasyBLEBuilder {
        Inspector.requireNonNull<ScannerType?>(scannerType, "scannerType can't be null")
        this.scannerType = scannerType
        return this
    }

    fun setExecutorService(executorService: ExecutorService?): EasyBLEBuilder {
        Inspector.requireNonNull<ExecutorService?>(executorService, "executorService can't be null")
        this.executorService = executorService
        return this
    }

    fun setDeviceCreator(deviceCreator: DeviceCreator?): EasyBLEBuilder {
        Inspector.requireNonNull<DeviceCreator?>(deviceCreator, "deviceCreator can't be null")
        this.deviceCreator = deviceCreator
        return this
    }

    fun setBondController(bondController: BondController?): EasyBLEBuilder {
        Inspector.requireNonNull<BondController?>(bondController, "bondController can't be null")
        this.bondController = bondController
        return this
    }

    fun setMethodDefaultThreadMode(mode: ThreadMode): EasyBLEBuilder {
        Inspector.requireNonNull<ThreadMode?>(mode, "mode can't be null")
        methodDefaultThreadMode = mode
        return this
    }

    fun setScanConfiguration(scanConfiguration: ScanConfiguration?): EasyBLEBuilder {
        Inspector.requireNonNull<ScanConfiguration?>(scanConfiguration, "scanConfiguration can't be null")
        this.scanConfiguration = scanConfiguration
        return this
    }

    fun setLogger(logger: Logger?): EasyBLEBuilder {
        Inspector.requireNonNull<Logger?>(logger, "logger can't be null")
        this.logger = logger
        return this
    }

    fun setObservable(observable: Observable?): EasyBLEBuilder {
        Inspector.requireNonNull<Observable?>(observable, "observable can't be null")
        this.observable = observable
        return this
    }

    fun setObserveAnnotationRequired(observeAnnotationRequired: Boolean): EasyBLEBuilder {
        isObserveAnnotationRequired = observeAnnotationRequired
        return this
    }

    fun setUseNordicBleBackend(useNordicBackend: Boolean): EasyBLEBuilder {
        this.useNordicBleBackend = useNordicBackend
        return this
    }

    fun build(): EasyBLE? {
        synchronized(EasyBLE::class.java) {
            if (EasyBLE.Companion.instance != null) {
                throw EasyBLEException("EasyBLE instance already exists. It can only be instantiated once.")
            }
            EasyBLE.Companion.instance = EasyBLE(this)
            return EasyBLE.Companion.instance
        }
    }

    companion object {
        private val DEFAULT_EXECUTOR_SERVICE: ExecutorService? = Executors.newCachedThreadPool()
    }
}
