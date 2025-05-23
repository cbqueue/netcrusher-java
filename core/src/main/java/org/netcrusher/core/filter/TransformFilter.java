package org.netcrusher.core.filter;

import java.nio.ByteBuffer;

/**
 * <p>Filter for transferred data. Filtering is made in reactor's thread so all blocking I/O should be made in other
 * thread with the copy of the input buffer.</p>
 *
 * <p>Filter that filters nothing:</p>
 * <pre>
 * void transform(ByteBuffer bb) {
 *     // no op
 * }
 * </pre>
 *
 * <p>Filter that inverses all bytes:</p>
 * <pre>
 * void transform(ByteBuffer bb) {
 *     if (bb.hasArray()) {
 *         final byte[] bytes = bb.array();
 *         final int offset = bb.arrayOffset() + bb.position();
 *         final int limit = bb.arrayOffset() + bb.limit();
 *         for (int i = offset; i < limit; i++) {
 *             bytes[i] = (byte) ~bytes[i];
 *         }
 *     } else {
 *         for (int i = 0, limit = bb.limit; i < limit; i++) {
 *             bb.put(i, (byte) ~bb.get(i));
 *         }
 *     }
 * }
 *</pre>
 */
@FunctionalInterface
public interface TransformFilter {

    TransformFilter NOOP = bb -> { };

    /**
     * <p>Callback that filters input byte buffer and return output byte buffer</p>
     * <p><em>Verify that both bb.position() and bb.limit() are properly set after method returns</em></p>
     * @param bb Input byte buffer with position set to 0 and limit set to buffer size
     */
    void transform(ByteBuffer bb);

}
