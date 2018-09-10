package hu.nemi.abcstore.sample.todo.utils

import android.arch.lifecycle.ViewModel
import dagger.MapKey
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val type: KClass<out ViewModel>)