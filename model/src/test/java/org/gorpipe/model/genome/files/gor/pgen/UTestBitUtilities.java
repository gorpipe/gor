package org.gorpipe.model.genome.files.gor.pgen;

import org.gorpipe.model.genome.files.gor.BitStream;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.gorpipe.model.genome.files.gor.pgen.BitUtilities.*;

public class UTestBitUtilities {

    @Test
    public void test_write1Bit() {
        final int[] input = getRandomIntegers(8, 2);
        byte[] buffer = new byte[1];
        for (int i = 0; i < input.length; ++i) {
            Assert.assertEquals(buffer.length, write1Bit(buffer, 0, buffer.length, input, i));
            final BitStream bs = new BitStream(1, buffer, 0);
            for (int j = i; j < input.length; ++j) {
                Assert.assertEquals(input[j], bs.next());
            }
        }
    }

    @Test
    public void test_write2Bits() {
        final int[] input = getRandomIntegers(16, 4);
        byte[] buffer = new byte[4];
        for (int i = 0; i < input.length; ++i) {
            Assert.assertEquals(buffer.length - (i / 4), write2Bits(buffer, 0, buffer.length, input, i));
            final BitStream bs = new BitStream(2, buffer, 0);
            for (int j = i; j < input.length; ++j) {
                Assert.assertEquals(input[j], bs.next());
            }
        }
    }

    @Test
    public void test_write4Bits() {
        final int[] input = getRandomIntegers(64, 16);
        byte[] buffer = new byte[32];
        for (int i = 0; i < input.length; ++i) {
            Assert.assertEquals(buffer.length - (i / 2), write4Bits(buffer, 0, buffer.length, input, i));
            final BitStream bs = new BitStream(4, buffer, 0);
            for (int j = i; j < input.length; ++j) {
                Assert.assertEquals(input[j], bs.next());
            }
        }
    }

    @Test
    public void test_write8Bits() {
        final int[] input = getRandomIntegers(1024, 256);
        byte[] buffer = new byte[1024];
        for (int i = 0; i < input.length; ++i) {
            Assert.assertEquals(buffer.length - i, write8Bits(buffer, 0, buffer.length, input, i));
            final BitStream bs = new BitStream(8, buffer, 0);
            for (int j = i; j < input.length; ++j) {
                Assert.assertEquals(input[j], bs.next());
            }
        }
    }

    @Test
    public void test_write16Bits() {
        final int[] input = getRandomIntegers(512, 65_536);
        byte[] buffer = new byte[1024];
        for (int i = 0; i < input.length; ++i) {
            Assert.assertEquals(buffer.length - 2 * i, write16Bits(buffer, 0, buffer.length, input, i));
            final BitStream bs = new BitStream(16, buffer, 0);
            for (int j = i; j < input.length; ++j) {
                Assert.assertEquals(input[j], bs.next());
            }
        }
    }

    @Test
    public void test_write24Bits() {
        final int[] input = getRandomIntegers(512, 16_777_216);
        byte[] buffer = new byte[1536];
        for (int i = 0; i < input.length; ++i) {
            Assert.assertEquals(buffer.length - 3 * i, write24Bits(buffer, 0, buffer.length, input, i));
            final BitStream bs = new BitStream(24, buffer, 0);
            for (int j = i; j < input.length; ++j) {
                Assert.assertEquals(input[j], bs.next());
            }
        }
    }

    @Test
    public void test_writeBoolArray() {
        final boolean[] input = new boolean[8];
        final byte[] buffer = new byte[1];
        for (int i = 0; i < input.length; ++i) {
            input[i] = Math.random() > 0.5;
        }

        for (int i = 0; i < input.length; ++i) {
            Assert.assertEquals(1, writeBoolArray(buffer, 0, buffer.length, input, i));
            final BitStream bs = new BitStream(1, buffer, 0);
            for (int j = i; j < input.length; ++j) {
                if (input[j]) Assert.assertEquals(1, bs.next());
                else Assert.assertEquals(0, bs.next());
            }
        }
    }

    private static int[] getRandomIntegers(int numbers, int upperBound) {
        final Random r = new Random();
        final int[] toReturn = new int[numbers];
        for (int i = 0; i < numbers; ++i) {
            toReturn[i] = r.nextInt(upperBound);
        }
        return toReturn;
    }
}
