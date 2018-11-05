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
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "ObjectPropertyName", "FunctionName")
package opus

import com.sun.jna.Platform
import opus.exceptions.OpusNotLoadedException
import opus.internal.*

/**
 * Frontend for the opus-kotlin API.
 */
@ExperimentalOpus
object Opus {
    const val LibProperty = "opus.lib"
    const val AutoloadProperty = "opus.autoload"

    private lateinit var _lib: LibOpus // instance of LibOpus

    internal val lib: LibOpus get() {
        if(!::_lib.isInitialized) {
            if(System.getProperty(Opus.AutoloadProperty, "false")!!.toBoolean()) {
                initialize()
                return _lib
            }
            throw OpusNotLoadedException("Opus was not initialized! Try running Opus.initialize()!")
        }

        return _lib
    }

    val isInitialized get() = Library.initialized

    /**
     * Initializes libopus for the current runtime.
     *
     * This must be called before any other functions are called,
     * otherwise they will throw an [OpusNotLoadedException].
     *
     * Alternatively, the system property `opus.autoload` may be set to `true`
     * to automatically load the opus native library when needed for the first
     * time, however this is not recommended for anything other than testing.
     *
     * Note that multiple invocations of this function do not do anything.
     */
    fun initialize(): Boolean {
        if(Library.loadFromJar()) {
            _lib = load(from = Library.nativesRoot)
            return true
        }
        return false
    }

    /** Gets the version of Opus being used. */
    val version: String get() = lib.opus_get_version_string()

    /**
     * Gets a string name for the provided [error] integer.
     *
     * Please note that due to the nature of the function
     * this may be inaccurate for some values depending
     * on what version of libopus you are using.
     */
    fun strError(error: Int): String = lib.opus_strerror(error)

    fun encoderSize(channels: Int): Int = lib.opus_encoder_get_size(channels)

    object Constants {
        const val GET_LSB_DEPTH_REQUEST = 4037
        const val GET_APPLICATION_REQUEST = 4001
        const val GET_FORCE_CHANNELS_REQUEST = 4023
        const val GET_VBR_REQUEST = 4007
        const val GET_BANDWIDTH_REQUEST = 4009
        const val SET_BITRATE_REQUEST = 4002
        const val SET_BANDWIDTH_REQUEST = 4008
        const val SIGNAL_MUSIC = 3002
        const val RESET_STATE = 4028
        const val FRAMESIZE_2_5_MS = 5001
        const val GET_COMPLEXITY_REQUEST = 4011
        const val FRAMESIZE_40_MS = 5005
        const val SET_PACKET_LOSS_PERC_REQUEST = 4014
        const val GET_VBR_CONSTRAINT_REQUEST = 4021
        const val SET_INBAND_FEC_REQUEST = 4012
        const val APPLICATION_RESTRICTED_LOWDELAY = 2051
        const val BANDWIDTH_FULLBAND = 1105
        const val SET_VBR_REQUEST = 4006
        const val BANDWIDTH_SUPERWIDEBAND = 1104
        const val SET_FORCE_CHANNELS_REQUEST = 4022
        const val APPLICATION_VOIP = 2048
        const val SIGNAL_VOICE = 3001
        const val GET_FINAL_RANGE_REQUEST = 4031
        const val BUFFER_TOO_SMALL = -2
        const val SET_COMPLEXITY_REQUEST = 4010
        const val FRAMESIZE_ARG = 5000
        const val GET_LOOKAHEAD_REQUEST = 4027
        const val GET_INBAND_FEC_REQUEST = 4013
        const val BITRATE_MAX = -1
        const val FRAMESIZE_5_MS = 5002
        const val BAD_ARG = -1
        const val GET_PITCH_REQUEST = 4033
        const val SET_SIGNAL_REQUEST = 4024
        const val FRAMESIZE_20_MS = 5004
        const val APPLICATION_AUDIO = 2049
        const val GET_DTX_REQUEST = 4017
        const val FRAMESIZE_10_MS = 5003
        const val SET_LSB_DEPTH_REQUEST = 4036
        const val UNIMPLEMENTED = -5
        const val GET_PACKET_LOSS_PERC_REQUEST = 4015
        const val INVALID_STATE = -6
        const val SET_EXPERT_FRAME_DURATION_REQUEST = 4040
        const val FRAMESIZE_60_MS = 5006
        const val GET_BITRATE_REQUEST = 4003
        const val INTERNAL_ERROR = -3
        const val SET_MAX_BANDWIDTH_REQUEST = 4004
        const val SET_VBR_CONSTRAINT_REQUEST = 4020
        const val GET_MAX_BANDWIDTH_REQUEST = 4005
        const val BANDWIDTH_NARROWBAND = 1101
        const val SET_GAIN_REQUEST = 4034
        const val SET_PREDICTION_DISABLED_REQUEST = 4042
        const val SET_APPLICATION_REQUEST = 4000
        const val SET_DTX_REQUEST = 4016
        const val BANDWIDTH_MEDIUMBAND = 1102
        const val GET_SAMPLE_RATE_REQUEST = 4029
        const val GET_EXPERT_FRAME_DURATION_REQUEST = 4041
        const val AUTO = -1000
        const val GET_SIGNAL_REQUEST = 4025
        const val GET_LAST_PACKET_DURATION_REQUEST = 4039
        const val GET_PREDICTION_DISABLED_REQUEST = 4043
        const val GET_GAIN_REQUEST = 4045
        const val BANDWIDTH_WIDEBAND = 1103
        const val INVALID_PACKET = -4
        const val MEMORY_ALLOCATION_FAILED = -7
        const val OK = 0
        const val MULTISTREAM_GET_DECODER_STATE_REQUEST = 5122
        const val MULTISTREAM_GET_ENCODER_STATE_REQUEST = 5120
    }

    internal object Library {
        var initialized = false

        var nativesRoot: String
            get() = System.getProperty(LibProperty) ?: defaultNativeRoot()
            set(value) { System.setProperty("opus.lib", value) }

        private fun defaultNativeRoot(): String {
            val platform = Platform.RESOURCE_PREFIX
            val ext = when(platform) {
                "darwin" -> "dylib"
                "linux-arm", "linux-aarch64", "linux-x86", "linux-x86-64" -> "so"
                "win32-x86", "win32-x86-64" -> "dll"

                else -> throw UnsupportedOperationException("Platform not supported: $platform")
            }

            return "/natives/$platform/libopus.$ext"
        }

        @Synchronized internal fun loadFromJar(): Boolean {
            if(initialized) return false

            val tmpRoot = defaultNativeRoot()
            loadLibraryFromJar(tmpRoot)
            nativesRoot = tmpRoot
            initialized = true
            return true
        }
    }
}

@ExperimentalOpus
fun OpusDecoder(sampleRate: Int, channels: Int): OpusDecoder =
    OpusDecoderImpl(sampleRate, channels)

@ExperimentalOpus
fun OpusEncoder(sampleRate: Int, channels: Int, application: Int): OpusEncoder =
    OpusEncoderImpl(sampleRate, channels, application)
