/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2020  Karl Schreiner, c4akarl@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.androidheads.vienna.escalero

import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.Charset

class Braf @Throws(IOException::class)
constructor(filename: String, mode: String, bufsize: Int) : RandomAccessFile(filename, mode) {

    internal val tag = "Braf"

    val nextLine: String?
        @Throws(IOException::class)
        get() {
            val charset: Charset = Charsets.UTF_8
            if (bufEnd - bufPos <= 0) {
                if (fillBuffer() < 0)
                    return null
            }
            var lineend = -1
            for (i in bufPos until bufEnd) {
                if (buffer[i] == '\n'.toByte()) {
                    lineend = i
                    break
                }
            }
            if (lineend < 0) {
                val input = StringBuffer(256)
                var c = read()
                while (c != -1 && c != '\n'.toInt()) {
                    input.append(c.toChar())
                    c = read()
                }
                return if (c == -1 && input.isEmpty()) null else input.toString()
            }
            val str =
                if (lineend > 0 && buffer[lineend - 1] == '\r'.toByte())
                    String(buffer, bufPos, lineend - bufPos - 1, charset)
                else
                    String(buffer, bufPos, lineend - bufPos, charset)
            bufPos = lineend + 1
            return str
        }

    private var buffer: ByteArray
    private var bufSize = 100000
    private var bufEnd = 0
    private var bufPos = 0
    private var realPos: Long = 0

    init {
        invalidate()
        bufSize = bufsize
        buffer = ByteArray(bufSize)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (bufPos >= bufEnd) {
            if (fillBuffer() < 0)
                return -1
        }
        return if (bufEnd == 0)
            -1
        else
            buffer[bufPos++].toInt()
    }

    @Throws(IOException::class)
    private fun fillBuffer(): Int {
        val n = super.read(buffer, 0, bufSize)
        if (n >= 0) {
            realPos += n.toLong()
            bufEnd = n
            bufPos = 0
        }
        return n
    }

    @Throws(IOException::class)
    private fun invalidate() {
        bufEnd = 0
        bufPos = 0
        realPos = super.getFilePointer()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val leftover = bufEnd - bufPos
        if (len <= leftover) {
            System.arraycopy(buffer, bufPos, b, off, len)
            bufPos += len
            return len
        }
        for (i in 0 until len) {
            val c = this.read()
            if (c != -1)
                b[off + i] = c.toByte()
            else
                return i
        }
        return len
    }

    @Throws(IOException::class)
    override fun getFilePointer(): Long {
        val l = realPos
        return l - bufEnd + bufPos
    }

    @Throws(IOException::class)
    override fun seek(pos: Long) {
        val n = (realPos - pos).toInt()
        if (n in 0..bufEnd)
            bufPos = bufEnd - n
        else {
            super.seek(pos)
            invalidate()
        }
    }

}
