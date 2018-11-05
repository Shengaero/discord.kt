/*
 * Copyright 2018 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("LibraryUtil")
package opus.internal

import com.sun.jna.Library
import com.sun.jna.Native
import java.io.File
import java.io.FileNotFoundException

internal inline fun <reified T: Library> load(from: String): T = Native.load(from, T::class.java)

internal fun loadLibraryFromJar(resource: String) {
    require(resource.startsWith('/')) { "The resource has to be absolute (start with '/')." }

    val lastPathSep = resource.lastIndexOf('/')
    val filename = resource.takeIf { lastPathSep != -1 }?.substring(lastPathSep)

    requireNotNull(filename) { "Could not determine filename from resource: $resource" }

    val parts = filename.split('.', limit = 2)
    val prefix = parts[0]
    val suffix = parts.lastOrNull()?.let { ".$it" }

    require(prefix.length >= 3) { "The filename has to be at least 3 characters long." }

    val opusClassLoader = Class.forName("opus.internal.LibraryUtil") // this class
    val nativeResource = opusClassLoader.getResource(resource) ?:
                 throw FileNotFoundException("File $resource was not found inside JAR.")

    val temp = createTempFile(prefix, suffix).also(File::deleteOnExit)

    if(!temp.exists()) throw FileNotFoundException("File ${temp.absolutePath} does not exist.")

    temp.outputStream().use { output ->
        nativeResource.openStream().use { input ->
            output.write(input.readBytes())
        }
    }

    System.load(temp.absolutePath)
}
