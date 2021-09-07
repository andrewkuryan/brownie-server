package com.gitlab.andrewkuryan.brownie

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import kotlin.reflect.KClass
import kotlin.jvm.internal.Reflection
import kotlin.reflect.jvm.javaType

@Target(AnnotationTarget.FIELD)
annotation class BackendField

class BackendExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>?) = false

    override fun shouldSkipField(f: FieldAttributes?) = f?.getAnnotation(BackendField::class.java) != null
}

class CustomGsonConverter : ContentConverter {

    private val gson = GsonBuilder()
            .addSerializationExclusionStrategy(BackendExclusionStrategy())
            .registerTypeAdapterFactory(SealedClassTypeAdapterFactory())
            .create()
    private val originalGsonConverter = GsonConverter(gson)

    override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType, value: Any): Any {
        return TextContent(
                gson.toJson(value, context.context.response.responseType?.javaType),
                contentType.withCharset(context.call.suitableCharset())
        )
    }

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>) =
            originalGsonConverter.convertForReceive(context)
}

class SealedClassTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        val kclass = Reflection.getOrCreateKotlinClass(type.rawType)
        return if (kclass.sealedSubclasses.any()) {
            SealedClassTypeAdapter(kclass, gson)
        } else {
            gson.getDelegateAdapter(this, type)
        }
    }
}

class SealedClassTypeAdapter<T : Any>(private val kclass: KClass<Any>, private val gson: Gson) : TypeAdapter<T>() {
    override fun read(jsonReader: JsonReader): T? {
        jsonReader.beginObject()
        jsonReader.nextName()
        val typeName = jsonReader.nextString()
        val innerClass = kclass.sealedSubclasses.firstOrNull {
            it.simpleName!!.contains(typeName)
        } ?: throw Exception("$typeName is not found to be a data class of the sealed class ${kclass.qualifiedName}")
        jsonReader.nextName()
        val result = gson.fromJson<T>(jsonReader, innerClass.javaObjectType)
        jsonReader.endObject()
        return result
    }

    override fun write(out: JsonWriter, value: T) {
        out.beginObject()
        out.name("type").value(value.javaClass.simpleName)
        out.name("data").jsonValue(gson.toJson(value))
        out.endObject()
    }
}