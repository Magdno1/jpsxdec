/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.discitems.savers;

import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;

/** Used to ensure the writing of audio samples matches the timing of the
 * reading of audio samples. */
public class AudioSync {

    private final int _iFirstPresentationSector;

    private final int _iSectorsPerSecond;
    private final int _iSamplesPerSecond;

    @Nonnull
    private final Fraction _samplesPerSector;

    public AudioSync(int iFirstPresentationSector,
                     int iSectorsPerSecond,
                     int iSamplesPerSecond)
    {
        _iSectorsPerSecond = iSectorsPerSecond;
        _iSamplesPerSecond = iSamplesPerSecond;
        // samples/sector = samples/second / sectors/second
        _samplesPerSector = new Fraction(_iSamplesPerSecond, _iSectorsPerSecond);

        _iFirstPresentationSector = iFirstPresentationSector;
    }

    public int getSectorsPerSecond() {
        return _iSectorsPerSecond;
    }

    public int getSamplesPerSecond() {
        return _iSamplesPerSecond;
    }

    public @Nonnull Fraction getSamplesPerSector() {
        return _samplesPerSector;
    }

    public long calculateAudioToCatchUp(@Nonnull Fraction audioPresentationSector,
                                        long lngSamplesWritten)
    {
        Fraction presentationTime = audioPresentationSector.subtract(_iFirstPresentationSector).divide(_iSectorsPerSecond);
        Fraction movieTime = new Fraction(lngSamplesWritten, _iSamplesPerSecond);
        Fraction timeDiff = presentationTime.subtract(movieTime);
        Fraction sampleDiff = timeDiff.multiply(_iSamplesPerSecond);

        long lngSampleDifference = Math.round(sampleDiff.asDouble());

        return lngSampleDifference;
    }

}
