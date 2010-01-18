/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.psx.video.decode;

import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.plugins.psx.video.encode.BitStreamWriter;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

/** Final Fantasy 7 video uncompressor.
 * Makes use of most of STR v2 code.
 * Just adds handling for camera data at the start of the frame.
 */
public class DemuxFrameUncompressor_FF7 extends DemuxFrameUncompressor_STRv2 {

    private static final Logger log = Logger.getLogger(DemuxFrameUncompressor_FF7.class.getName());
    protected Logger getLog() { return log; }

    public DemuxFrameUncompressor_FF7() {
        super();
    }
    public DemuxFrameUncompressor_FF7(byte[] abDemuxData) throws NotThisTypeException {
        super(abDemuxData);
    }

    private boolean _blnHasCameraData;

    public byte[] getCameraData() {
        if (_blnHasCameraData) {
            return Misc.copyOfRange(_bitReader.getArray(), 0, 40);
        } else {
            return null;
        }
    }

    public long getMagic3800() {
        return _lngMagic3800;
    }

    public int getHalfVlcCountCeil32() {
        return _iHalfVlcCountCeil32;
    }


    @Override
    protected ArrayBitReader readHeader(byte[] abFrameData) throws NotThisTypeException {
        int iStartOffset = 0;
        _lngMagic3800    = IO.readUInt16LE(abFrameData, 2);

        _blnHasCameraData = _lngMagic3800 != 0x3800;
        if (_blnHasCameraData) {
            iStartOffset = 40;
            _lngMagic3800 = IO.readUInt16LE(abFrameData, iStartOffset + 2);
        }

        _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, iStartOffset + 0);
        _iQscale             = IO.readSInt16LE(abFrameData, iStartOffset + 4);
        int iVersion         = IO.readSInt16LE(abFrameData, iStartOffset + 6);

        if (_lngMagic3800 != 0x3800 || _iQscale < 1 ||
            iVersion != 1 || _iHalfVlcCountCeil32 < 0)
            throw new NotThisTypeException();

        return new ArrayBitReader(abFrameData, true, iStartOffset + 8);
    }

    public static boolean checkHeader(byte[] abFrameData) {
        int iStartOffset = 0;
        long lngMagic3800       = IO.readUInt16LE(abFrameData, 2);

        if (lngMagic3800 != 0x3800) {
            iStartOffset = 40;
            lngMagic3800        = IO.readUInt16LE(abFrameData, iStartOffset + 2);
        }

        int iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, iStartOffset + 0);
        int iQscale             = IO.readSInt16LE(abFrameData, iStartOffset + 4);
        int iVersion            = IO.readSInt16LE(abFrameData, iStartOffset + 6);

        return !(lngMagic3800 != 0x3800 || iQscale < 1 ||
                 iVersion != 1 || iHalfVlcCountCeil32 < 0);
    }

    @Override
    public String toString() {
        return "FF7";
    }


    public static class Recompressor_FF7 extends DemuxFrameUncompressor_STRv2.FrameRecompressor_STRv2 {

        public void compressToDemuxFF7(BitStreamWriter oBitStream, int iQscale, int iVlcCount) throws IOException {
            compressToDemuxFF7(oBitStream, iQscale, iVlcCount, null);
        }

        public void compressToDemuxFF7(BitStreamWriter bitStream, int iQscale, int iVlcCount, byte[] abCameraData)
                throws IOException
        {
            if (iQscale < 1 || iVlcCount < 0) throw new IllegalArgumentException();
            _iQscale = iQscale;
            _iVlcCount = iVlcCount;

            _bitStream = bitStream;
            if (abCameraData != null) {
                _bitStream.setLittleEndian(false);
                _bitStream.write(abCameraData);
            }
            _bitStream.setLittleEndian(true);
            _bitStream.writeInt16LE( (((iVlcCount+1) / 2) + 31) & ~31 );
            _bitStream.writeInt16LE(0x3800);
            _bitStream.writeInt16LE(iQscale);
            _bitStream.writeInt16LE(1);
        }
    }

}