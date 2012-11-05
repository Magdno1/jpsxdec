/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012  Michael Sabin
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

import java.awt.BorderLayout;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import jpsxdec.SavingGuiTable;


public class VideoSaverBuilderStrGui extends VideoSaverBuilderGui<VideoSaverBuilderStr> {



    public VideoSaverBuilderStrGui(VideoSaverBuilderStr writerBuilder) {
        super(writerBuilder);

        addListeners(new EmulateAv(), new ParallelAudio());
    }

    
    private class EmulateAv extends AbstractCheck {
        public EmulateAv() { super("Emulate PSX a/v sync:"); }
        public boolean isSelected() {
            return _writerBuilder.getEmulatePsxAvSync();
        }
        public void setSelected(boolean b) {
            _writerBuilder.setEmulatePsxAVSync(b);
        }
        public boolean isEnabled() {
            return _writerBuilder.getEmulatePsxAVSync_enabled();
        }
    }


    private enum COLUMNS {
        Save() {
            public Class type() { return Boolean.class; }
            public boolean editable() { return true; }
            public Object get(VideoSaverBuilderStr bldr, int i) {
                return bldr.getParallelAudio_selected(i);
            }
            public void set(VideoSaverBuilderStr bldr, int i, Object val) {
                bldr.setParallelAudio(i, (Boolean)val);
            }
        },
        Num() {
            public Class type() { return Integer.class; }
            public Object get(VideoSaverBuilderStr bldr, int i) {
                return bldr.getParallelAudio(i).getIndexId().getListIndex();
            }
            public String toString() { return "#"; }
        },
        Id() {
            public Class type() { return String.class; }
            public Object get(VideoSaverBuilderStr bldr, int i) {
                return bldr.getParallelAudio(i).getIndexId().getTopLevel();
            }
            public String toString() { return ""; }
        },
        Details() {
            public Class type() { return String.class; }
            public Object get(VideoSaverBuilderStr bldr, int i) {
                return bldr.getParallelAudio(i).getInterestingDescription();
            }
        };
        abstract public Class type();
        public boolean editable() { return false; }
        abstract public Object get(VideoSaverBuilderStr bldr, int i);
        public void set(VideoSaverBuilderStr bldr, int i, Object val) {}
    }
    private class ParallelAudio extends AbstractTableModel implements ChangeListener {

        JTable __tbl;

        public ParallelAudio() {
            __tbl = new JTable(this);
            __tbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            add(new JScrollPane(__tbl), BorderLayout.CENTER);
            SavingGuiTable.autoResizeColWidth(__tbl);
        }

        public int getRowCount() {
            return _writerBuilder.getParallelAudioCount();
        }

        public int getColumnCount() {
            return COLUMNS.values().length;
        }

        public String getColumnName(int columnIndex) {
            return COLUMNS.values()[columnIndex].toString();
        }

        public Class<?> getColumnClass(int columnIndex) {
            return COLUMNS.values()[columnIndex].type();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return COLUMNS.values()[columnIndex].editable();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return COLUMNS.values()[columnIndex].get(_writerBuilder, rowIndex);
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            COLUMNS.values()[columnIndex].set(_writerBuilder, rowIndex, aValue);
        }

        public void stateChanged(ChangeEvent e) {
            __tbl.setEnabled(_writerBuilder.getParallelAudio_enabled());
            this.fireTableDataChanged();
        }

    }

}