package com.mpdc4gsr.ble.core

import com.mpdc4gsr.ble.core.util.Logger
import com.mpdc4gsr.commons.observer.Observable
import com.mpdc4gsr.commons.poster.ThreadMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Builder class for creating EasyBLE instances
 */
class EasyBLEBuilder {
    var bondController: BondController? = null
        private set
    
    var scannerType: ScannerType? = null
        private set
    
    var useNordicBleBackend: Boolean = false
        private set
    
    var deviceCreator: DeviceCreator? = null
        private set
    
    var scanConfiguration: ScanConfiguration? = null
        private set
    
    var logger: Logger? = null
        private set
    
    var observable: Observable? = null
        private set
    
    var executorService: ExecutorService = Executors.newCachedThreadPool()
        private set
    
    var methodDefaultThreadMode: ThreadMode = ThreadMode.POSTING
        private set
    
    var isObserveAnnotationRequired: Boolean = false
        private set

    /**
     * Set the bond controller for managing device bonding
     */
    fun setBondController(bondController: BondController?): EasyBLEBuilder {
        this.bondController = bondController
        return this
    }

    /**
     * Set the scanner type for BLE scanning
     */
    fun setScannerType(scannerType: ScannerType?): EasyBLEBuilder {
        this.scannerType = scannerType
        return this
    }

    /**
     * Enable or disable Nordic BLE backend
     */
    fun setUseNordicBleBackend(useNordicBleBackend: Boolean): EasyBLEBuilder {
        this.useNordicBleBackend = useNordicBleBackend
        return this
    }

    /**
     * Set the device creator for creating Device instances
     */
    fun setDeviceCreator(deviceCreator: DeviceCreator?): EasyBLEBuilder {
        this.deviceCreator = deviceCreator
        return this
    }

    /**
     * Set the scan configuration
     */
    fun setScanConfiguration(scanConfiguration: ScanConfiguration?): EasyBLEBuilder {
        this.scanConfiguration = scanConfiguration
        return this
    }

    /**
     * Set the logger instance
     */
    fun setLogger(logger: Logger?): EasyBLEBuilder {
        this.logger = logger
        return this
    }

    /**
     * Set the observable instance
     */
    fun setObservable(observable: Observable?): EasyBLEBuilder {
        this.observable = observable
        return this
    }

    /**
     * Set the executor service for background operations
     */
    fun setExecutorService(executorService: ExecutorService): EasyBLEBuilder {
        this.executorService = executorService
        return this
    }

    /**
     * Set the default thread mode for method execution
     */
    fun setMethodDefaultThreadMode(threadMode: ThreadMode): EasyBLEBuilder {
        this.methodDefaultThreadMode = threadMode
        return this
    }

    /**
     * Set whether observe annotation is required
     */
    fun setObserveAnnotationRequired(required: Boolean): EasyBLEBuilder {
        this.isObserveAnnotationRequired = required
        return this
    }

    /**
     * Build the EasyBLE instance
     */
    fun build(): EasyBLE {
        return EasyBLE(this)
    }
}