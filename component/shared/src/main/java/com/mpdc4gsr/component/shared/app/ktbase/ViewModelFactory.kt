package com.mpdc4gsr.component.shared.app.ktbase

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class BaseViewModelFactory(
    private val application: Application,
    private val repositories: Map<Class<*>, Any> = emptyMap(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T =
        when {
            modelClass.isAssignableFrom(BaseViewModel::class.java) -> {
                BaseViewModel() as T
            }

            else -> {
                try {
                    // Try to create with application context
                    val constructor = modelClass.getDeclaredConstructor(Application::class.java)
                    constructor.newInstance(application) as T
                } catch (e: NoSuchMethodException) {
                    try {
                        // Try to create with repositories
                        createWithRepositories(modelClass)
                    } catch (e: Exception) {
                        // Fallback to default constructor
                        modelClass.getDeclaredConstructor().newInstance() as T
                    }
                }
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ViewModel> createWithRepositories(modelClass: Class<T>): T {
        val constructors = modelClass.declaredConstructors
        for (constructor in constructors) {
            val parameterTypes = constructor.parameterTypes
            val parameters = mutableListOf<Any>()
            var canCreate = true
            for (paramType in parameterTypes) {
                when {
                    paramType == Application::class.java -> {
                        parameters.add(application)
                    }

                    repositories.containsKey(paramType) -> {
                        parameters.add(repositories[paramType]!!)
                    }

                    else -> {
                        canCreate = false
                        break
                    }
                }
            }
            if (canCreate) {
                return constructor.newInstance(*parameters.toTypedArray()) as T
            }
        }
        throw IllegalArgumentException("Cannot create ViewModel ${modelClass.simpleName}")
    }

    class Builder(
        private val application: Application,
    ) {
        private val repositories = mutableMapOf<Class<*>, Any>()

        fun <T : Any> addRepository(
            repositoryClass: Class<T>,
            repository: T,
        ): Builder {
            repositories[repositoryClass] = repository
            return this
        }

        inline fun <reified T : Any> addRepository(repository: T): Builder = addRepository(T::class.java, repository)

        fun build(): BaseViewModelFactory = BaseViewModelFactory(application, repositories)
    }
}

inline fun <reified T : ViewModel> androidx.lifecycle.ViewModelStoreOwner.createViewModelWithFactory(factory: BaseViewModelFactory): T =
    ViewModelProvider(this, factory)[T::class.java]


