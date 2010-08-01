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

package jpsxdec.modules.xa;

import jpsxdec.modules.JPSXModule;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.IndexingDemuxerIS;
import jpsxdec.modules.DiscItemSerialization;
import jpsxdec.modules.DiscItem;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.modules.psx.video.bitstreams.BitStreamUncompressor;

/**
 * Watches for XA audio streams.
 * Tracks the channel numbers and maintains all the XA streams.
 * Adds them to the media list as they end.
 *
 */
public class JPSXModuleXAAudio extends JPSXModule {

    private static final Logger log = Logger.getLogger(JPSXModuleXAAudio.class.getName());

    private static JPSXModuleXAAudio SINGLETON;

    public static JPSXModuleXAAudio getModule() {
        if (SINGLETON == null)
            SINGLETON = new JPSXModuleXAAudio();
        return SINGLETON;
    }

    /** Tracks the indexing of one audio stream in one channel. */
    private static class AudioStreamIndex {

        /** First sector of the audio stream. */
        private final int _iStartSector;

        /** Last sector before _currentXA that was a part of this stream. */
        private SectorXA _previousXA;
        /** Last sector (or 'current' sector, if you will) that was a part of this stream.
         Is never null. */
        private SectorXA _currentXA;

        /** Get the last (or 'current') sector that was part of this stream.
         May be null. */
        public SectorXA getCurrent() { return _currentXA; }

        /** Count of how many sample are found in the stream. */
        private long _lngSampleCount = 0;

        /** Number of sectors between XA sectors that are part of this stream.
         * Should only ever be 4, 8, 16, or 32. */
        private int _iAudioStride = -1;

        public AudioStreamIndex(SectorXA first) {
            _currentXA = first;
            _iStartSector = first.getSectorNumber();
        }

        /**
         * @return true if the sector was accepted as part of this stream.
         */
        public boolean sectorRead(SectorXA newCurrent) {
            // if the previous ('current') sector's EOF bit was set, this stream is closed
            // this is important for Silent Hill and R4 Ridge Racer
            if (_currentXA.getCDSector().getSubMode().getEofMarker())
                return false;

            if (newCurrent.matchesPrevious(_currentXA) == null)
                return false;

            // check the stride
            int iStride = newCurrent.getSectorNumber() - _currentXA.getSectorNumber();
            if (_iAudioStride < 0)
                _iAudioStride = iStride;
            else if (iStride != _iAudioStride)
                return false;

            _previousXA = _currentXA;
            _lngSampleCount += _previousXA.getSampleCount();
            _currentXA = newCurrent;

            return true; // the sector was accepted
        }

        public DiscItem createMediaItemFromCurrent() {
            if (_previousXA == null && _currentXA.isAllQuiet()) {
                if (log.isLoggable(Level.INFO))
                    log.info("Ignoring silent XA audio stream only 1 sector long at " + _iStartSector + " channel " + _currentXA.getChannel());
                return null;
            }
            _lngSampleCount += _currentXA.getSampleCount();
            return _currentXA.createMedia(_iStartSector, _iAudioStride, _lngSampleCount);
        }

        public DiscItem createMediaItemFromPrevious() {
            if (_previousXA == null) {
                if (log.isLoggable(Level.WARNING))
                    log.warning("Trying to create XA item from non-existant previous sector! Current sector is " + _iStartSector);
                return null;
            } else {
                return _currentXA.createMedia(_iStartSector, _iAudioStride, _lngSampleCount);
            }
        }

        public boolean ended(int iSectorNum) {
            return (_iAudioStride >= 0) &&
                   (iSectorNum > _currentXA.getSectorNumber() + _iAudioStride);
        }
    }


    AudioStreamIndex[] _aoChannels = new AudioStreamIndex[32];

    private JPSXModuleXAAudio() {
        
    }

    @Override
    public IdentifiedSector identifySector(CdSector oSect) {
        try { return new SectorXA(oSect); }
        catch (NotThisTypeException ex) {}
        try { return new SectorXANull(oSect); }
        catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void deserialize_lineRead(DiscItemSerialization oSerial) {
        try {
            if (DiscItemXAAudioStream.TYPE_ID.equals(oSerial.getType()))
                super.addDiscItem(new DiscItemXAAudioStream(oSerial));
        } catch (NotThisTypeException ex) {}
    }

    @Override
    public void indexing_sectorRead(IdentifiedSector sector) {
        if (sector instanceof SectorXA) {

            SectorXA audSect = (SectorXA)sector;

            AudioStreamIndex audStream = _aoChannels[audSect.getChannel()];
            if (audStream == null) {
                audStream = new AudioStreamIndex(audSect);
                _aoChannels[audSect.getChannel()] = audStream;
            } else if (!audStream.sectorRead(audSect)) {
                DiscItem item = audStream.createMediaItemFromCurrent();
                if (item != null)
                    super.addDiscItem(item);
                audStream = new AudioStreamIndex(audSect);
                _aoChannels[audSect.getChannel()] = audStream;
            }
        } else {
            // check for streams that are beyond their stride

            for (int i = 0; i < _aoChannels.length; i++) {
                AudioStreamIndex audStream = _aoChannels[i];
                if (audStream != null && audStream.ended(sector.getSectorNumber())) {
                    DiscItem item = audStream.createMediaItemFromCurrent();
                    if (item != null)
                        super.addDiscItem(item);
                    _aoChannels[i] = null;
                }
            }
        }

    }

    @Override
    public void indexing_endOfDisc() {
        for (int i = 0; i < _aoChannels.length; i++) {
            AudioStreamIndex audStream = _aoChannels[i];
            if (audStream != null) {
                DiscItem item = audStream.createMediaItemFromCurrent();
                if (item != null)
                    super.addDiscItem(item);
                _aoChannels[i] = null;
            }
        }
    }

    @Override
    public void indexing_static(IndexingDemuxerIS oIS) throws IOException {
        if (oIS.getSectorPosition() == 0) {

        }
    }

    public void indexing_endAllCurrent() {
        indexing_endOfDisc();
    }

    public void indexing_endAllBeforeCurrent() {
        for (int i = 0; i < _aoChannels.length; i++) {
            AudioStreamIndex audStream = _aoChannels[i];
            if (audStream != null) {
                DiscItem item = audStream.createMediaItemFromPrevious();
                if (item != null) {
                    super.addDiscItem(item);
                    SectorXA current = audStream.getCurrent();
                    _aoChannels[i] = new AudioStreamIndex(current);
                }
            }
        }
    }

    @Override
    public String getModuleDescription() {
        return "XA ADPCM audio decoding module for jPSXdec by Michael Sabin";
    }

    @Override
    public BitStreamUncompressor identifyVideoFrame(byte[] abHeaderBytes, long lngFrameNum) {
        return null;
    }

}