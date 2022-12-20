/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.io;


/**
 * Abstract class for writing to character streams.  The only methods that a
 * subclass must implement are write(char[], int, int), flush(), and close().
 * Most subclasses, however, will override some of the methods defined here in
 * order to provide higher efficiency, additional functionality, or both.
 *
 * @see Writer
 * @see   BufferedWriter
 * @see   CharArrayWriter
 * @see   FilterWriter
 * @see   OutputStreamWriter
 * @see     FileWriter
 * @see   PipedWriter
 * @see   PrintWriter
 * @see   StringWriter
 * @see Reader
 *
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public abstract class Writer implements Appendable, Closeable, Flushable {

    /**
     * Temporary buffer used to hold writes of strings and single characters
     */
    // 字符缓冲区
    private char[] writeBuffer;

    /**
     * Size of writeBuffer, must be >= 1
     */
    // 缓冲区大小
    private static final int WRITE_BUFFER_SIZE = 1024;

    /**
     * The object used to synchronize operations on this stream.  For
     * efficiency, a character-stream object may use an object other than
     * itself to protect critical sections.  A subclass should therefore use
     * the object in this field rather than <tt>this</tt> or a synchronized
     * method.
     */
    protected Object lock;

    /**
     * Creates a new character-stream writer whose critical sections will
     * synchronize on the writer itself.
     */
    protected Writer() {
        this.lock = this;
    }

    /**
     * Creates a new character-stream writer whose critical sections will
     * synchronize on the given object.
     *
     * @param  lock
     *         Object to synchronize on
     */
    protected Writer(Object lock) {
        if (lock == null) {
            throw new NullPointerException();
        }
        this.lock = lock;
    }

    /**
     * Writes a single character.  The character to be written is contained in
     * the 16 low-order bits of the given integer value; the 16 high-order bits
     * are ignored.
     *
     * <p> Subclasses that intend to support efficient single-character output
     * should override this method.
     *
     * @param  c
     *         int specifying a character to be written
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    /**
     *
     * 只写一个字符。
     * 待写入的字符包含在给定整数值的16位低阶比特中;
     * 16个高阶位被忽略。
     */
    public void write(int c) throws IOException {
        synchronized (lock) {
            if (writeBuffer == null){
                writeBuffer = new char[WRITE_BUFFER_SIZE];
            }
            // 先写入writeBuffer，再把writeBuffer写入输出流
            writeBuffer[0] = (char) c;
            write(writeBuffer, 0, 1);
        }
    }

    /**
     * Writes an array of characters.
     *
     * @param  cbuf
     *         Array of characters to be written
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    // 写入一个字符数组。
    public void write(char cbuf[]) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    /**
     * Writes a portion of an array of characters.
     *
     * @param  cbuf
     *         Array of characters
     *
     * @param  off
     *         Offset from which to start writing characters
     *
     * @param  len
     *         Number of characters to write
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    // 写入一个字符数组的一部分。
    abstract public void write(char cbuf[], int off, int len) throws IOException;

    /**
     * Writes a string.
     *
     * @param  str
     *         String to be written
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    // 写入一个字符串
    public void write(String str) throws IOException {
        write(str, 0, str.length());
    }

    /**
     * Writes a portion of a string.
     *
     * @param  str
     *         A String
     *
     * @param  off
     *         Offset from which to start writing characters
     *
     * @param  len
     *         Number of characters to write
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>off</tt> is negative, or <tt>len</tt> is negative,
     *          or <tt>off+len</tt> is negative or greater than the length
     *          of the given string
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
     // 写入一个字符串的一部分。
    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            char cbuf[];
            // 如果len小于等于WRITE_BUFFER_SIZE，cbuf为writeBuffer
            // 如果大于，建立一个len长度的数组，赋值给cbuf（注意：没有赋值给writeBuffer！)
            if (len <= WRITE_BUFFER_SIZE) {
                if (writeBuffer == null) {
                    writeBuffer = new char[WRITE_BUFFER_SIZE];
                }
                cbuf = writeBuffer;
            } else {    // Don't permanently allocate very large buffers.
                cbuf = new char[len];
            }
            // str的元素复制到cbuf，然后cbuf写入输出流
            str.getChars(off, (off + len), cbuf, 0);
            write(cbuf, 0, len);
        }
    }

    /**
     * Appends the specified character sequence to this writer.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq)</tt>
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.write(csq.toString()) </pre>
     *
     * <p> Depending on the specification of <tt>toString</tt> for the
     * character sequence <tt>csq</tt>, the entire sequence may not be
     * appended. For instance, invoking the <tt>toString</tt> method of a
     * character buffer will return a subsequence whose content depends upon
     * the buffer's position and limit.
     *
     * @param  csq
     *         The character sequence to append.  If <tt>csq</tt> is
     *         <tt>null</tt>, then the four characters <tt>"null"</tt> are
     *         appended to this writer.
     *
     * @return  This writer
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @since  1.5
     */
    // 向写入器追加指定的字符序列。
    public Writer append(CharSequence csq) throws IOException {
        if (csq == null)
            write("null");
        else
            write(csq.toString());
        return this;
    }

    /**
     * Appends a subsequence of the specified character sequence to this writer.
     * <tt>Appendable</tt>.
     *
     * <p> An invocation of this method of the form <tt>out.append(csq, start,
     * end)</tt> when <tt>csq</tt> is not <tt>null</tt> behaves in exactly the
     * same way as the invocation
     *
     * <pre>
     *     out.write(csq.subSequence(start, end).toString()) </pre>
     *
     * @param  csq
     *         The character sequence from which a subsequence will be
     *         appended.  If <tt>csq</tt> is <tt>null</tt>, then characters
     *         will be appended as if <tt>csq</tt> contained the four
     *         characters <tt>"null"</tt>.
     *
     * @param  start
     *         The index of the first character in the subsequence
     *
     * @param  end
     *         The index of the character following the last character in the
     *         subsequence
     *
     * @return  This writer
     *
     * @throws  IndexOutOfBoundsException
     *          If <tt>start</tt> or <tt>end</tt> are negative, <tt>start</tt>
     *          is greater than <tt>end</tt>, or <tt>end</tt> is greater than
     *          <tt>csq.length()</tt>
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @since  1.5
     */
    // 向写入器追加指定字符序列的子序列。
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        CharSequence cs = (csq == null ? "null" : csq);
        write(cs.subSequence(start, end).toString());
        return this;
    }

    /**
     * Appends the specified character to this writer.
     *
     * <p> An invocation of this method of the form <tt>out.append(c)</tt>
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     out.write(c) </pre>
     *
     * @param  c
     *         The 16-bit character to append
     *
     * @return  This writer
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @since 1.5
     */
    // 向写入器追加单个字符的子序列。
    public Writer append(char c) throws IOException {
        write(c);
        return this;
    }

    /**
     * Flushes the stream.  If the stream has saved any characters from the
     * various write() methods in a buffer, write them immediately to their
     * intended destination.  Then, if that destination is another character or
     * byte stream, flush it.  Thus one flush() invocation will flush all the
     * buffers in a chain of Writers and OutputStreams.
     *
     * <p> If the intended destination of this stream is an abstraction provided
     * by the underlying operating system, for example a file, then flushing the
     * stream guarantees only that bytes previously written to the stream are
     * passed to the operating system for writing; it does not guarantee that
     * they are actually written to a physical device such as a disk drive.
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    /**
     * 刷新流。
     * 如果流在缓冲区中保存了各种write()方法中的任何字符，请立即将它们写入预期的目的地。
     * 然后，如果目的地是另一个字符或字节流，刷新它。
     * 因此，一次flush()调用将刷新写入器和输出流链中的所有缓冲区。
     *
     * <p> 如果这个流的目的是底层操作系统提供的抽象，例如文件，
     * 那么刷新流只能保证先前写入到流的字节被传递给操作系统进行写入;
     * 它不能保证它们确实被写入物理设备，如磁盘驱动器。
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    abstract public void flush() throws IOException;

    /**
     * Closes the stream, flushing it first. Once the stream has been closed,
     * further write() or flush() invocations will cause an IOException to be
     * thrown. Closing a previously closed stream has no effect.
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    /**
     * 关闭流，首先flush。
     * 一旦流被关闭，进一步的write()或flush()调用将导致抛出IOException。
     * 关闭以前关闭的流没有任何效果。
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    abstract public void close() throws IOException;

}
