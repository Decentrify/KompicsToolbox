/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.ktoolbox.util.stream.buffer;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Config;
import se.sics.kompics.config.TypesafeConfig;
import se.sics.ktoolbox.util.reference.KReference;
import se.sics.ktoolbox.util.reference.KReferenceException;
import se.sics.ktoolbox.util.reference.KReferenceFactory;
import se.sics.ktoolbox.util.result.Result;
import se.sics.ktoolbox.util.stream.events.StreamWrite;
import se.sics.ktoolbox.util.stream.ranges.KBlock;
import se.sics.ktoolbox.util.stream.ranges.KBlockImpl;
import se.sics.ktoolbox.util.stream.test.StreamWriteReqEC;
import se.sics.ktoolbox.util.test.EventContentValidator;
import se.sics.ktoolbox.util.test.MockComponentProxy;
import se.sics.ktoolbox.util.test.MockExceptionHandler;
import se.sics.ktoolbox.util.test.MockStreamEndpoint;
import se.sics.ktoolbox.util.test.MockStreamPort;
import se.sics.ktoolbox.util.test.MockStreamResource;
import se.sics.ktoolbox.util.test.MockWC;
import se.sics.ktoolbox.util.test.PortValidator;
import se.sics.ktoolbox.util.test.Validator;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestSimpleAppendKBuffer {

    private static final Logger LOG = LoggerFactory.getLogger(TestSimpleAppendKBuffer.class);

    @Test
    public void simpleTest() throws KReferenceException {
        LOG.info("simple test");
        //setup
        Config config = TypesafeConfig.load();
        MockComponentProxy proxy = new MockComponentProxy();
        MockExceptionHandler syncExHandler = new MockExceptionHandler();
        MockStreamResource writeResource = new MockStreamResource("mock1");
        MockStreamEndpoint writeEndpoint = new MockStreamEndpoint();
        MockWC allWriteResult = new MockWC();

        long appendPos = 0;
        Validator validator;
        KReference<byte[]> ref1, ref2, ref3;
        StreamWrite.Request swr1, swr2, swr3;

        KBlock b1 = new KBlockImpl(0, 0, 2);
        ref1 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr1 = new StreamWrite.Request(writeResource, b1.lowerAbsEndpoint(), ref1.getValue().get());

        KBlock b2 = new KBlockImpl(2, 6, 8);
        ref2 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr2 = new StreamWrite.Request(writeResource, b2.lowerAbsEndpoint(), ref2.getValue().get());

        KBlock b3 = new KBlockImpl(1, 3, 5);
        ref3 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr3 = new StreamWrite.Request(writeResource, b3.lowerAbsEndpoint(), ref3.getValue().get());

        //settig up validators;
        proxy.expect(new PortValidator(MockStreamPort.class, false));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr1));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr3));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr2));

        //actual run
        SimpleAppendKBuffer sakBuf = new SimpleAppendKBuffer(config, proxy, syncExHandler, writeEndpoint, writeResource, 0);

        //write1
        sakBuf.write(b1, ref1, allWriteResult);
        ref1.release();
        Assert.assertTrue(ref1.isValid());
        //answer to write1
        sakBuf.handleWriteResp.handle(swr1.respond(Result.success(true)));
        Assert.assertFalse(ref1.isValid());
        Assert.assertTrue(sakBuf.isIdle());
        //write2
        sakBuf.write(b2, ref2, allWriteResult);
        ref2.release();
        Assert.assertTrue(ref2.isValid());
        //write3
        sakBuf.write(b3, ref3, allWriteResult);
        ref3.release();
        Assert.assertTrue(ref3.isValid());
        //answer to write3
        sakBuf.handleWriteResp.handle(swr3.respond(Result.success(true)));
        Assert.assertTrue(ref2.isValid());
        Assert.assertFalse(ref3.isValid());
        Assert.assertFalse(sakBuf.isIdle());
        //answer to write2
        sakBuf.handleWriteResp.handle(swr2.respond(Result.success(true)));
        Assert.assertFalse(ref2.isValid());
        Assert.assertTrue(sakBuf.isIdle());

        sakBuf.close();
        Assert.assertEquals(0, syncExHandler.getExceptionCounter());

        //validation
        validator = proxy.validate();
        if (validator != null) {
            Assert.fail(validator.toString());
        }
    }

    @Test
    public void closeBeforeFinish() throws KReferenceException {
        LOG.info("close before finish");
        //setup
        Config config = TypesafeConfig.load();
        MockComponentProxy proxy = new MockComponentProxy();
        MockExceptionHandler syncExHandler = new MockExceptionHandler();
        MockStreamEndpoint writeEndpoint = new MockStreamEndpoint();
        MockStreamResource writeResource = new MockStreamResource("mock1");
        MockWC allWriteResult = new MockWC();
        long appendPos = 0;
        Validator validator;
        KReference<byte[]> ref1, ref2, ref3;
        StreamWrite.Request swr1, swr2, swr3;

        KBlock b1 = new KBlockImpl(0, 0, 2);
        ref1 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr1 = new StreamWrite.Request(writeResource, b1.lowerAbsEndpoint(), ref1.getValue().get());

        KBlock b2 = new KBlockImpl(2, 6, 8);
        ref2 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr2 = new StreamWrite.Request(writeResource, b2.lowerAbsEndpoint(), ref2.getValue().get());

        KBlock b3 = new KBlockImpl(1, 3, 5);
        ref3 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr3 = new StreamWrite.Request(writeResource, b3.lowerAbsEndpoint(), ref3.getValue().get());

        //settig up validators;
        proxy.expect(new PortValidator(MockStreamPort.class, false));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr1));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr3));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr2));

        //actual run
        SimpleAppendKBuffer sakBuf = new SimpleAppendKBuffer(config, proxy, syncExHandler, writeEndpoint, writeResource, 0);

        //write1
        sakBuf.write(b1, ref1, allWriteResult);
        ref1.release();
        Assert.assertTrue(ref1.isValid());
        //answer to write1
        sakBuf.handleWriteResp.handle(swr1.respond(Result.success(true)));
        Assert.assertFalse(ref1.isValid());
        Assert.assertTrue(sakBuf.isIdle());
        //write2
        sakBuf.write(b2, ref2, allWriteResult);
        ref2.release();
        Assert.assertTrue(ref2.isValid());
        //write3
        sakBuf.write(b3, ref3, allWriteResult);
        ref3.release();
        Assert.assertTrue(ref3.isValid());
        //answer to write3
        sakBuf.handleWriteResp.handle(swr3.respond(Result.success(true)));
        Assert.assertTrue(ref2.isValid());
        Assert.assertFalse(ref3.isValid());
        Assert.assertFalse(sakBuf.isIdle());
        //close before answer to write2
        sakBuf.close();
        Assert.assertFalse(ref2.isValid());
        Assert.assertEquals(0, syncExHandler.getExceptionCounter());

        //validation
        validator = proxy.validate();
        if (validator != null) {
            Assert.fail(validator.toString());
        }
    }

    @Test
    public void errorOnWrite() throws KReferenceException {
        LOG.info("error on write");
        //setup
        Config config = TypesafeConfig.load();
        MockComponentProxy proxy = new MockComponentProxy();
        MockExceptionHandler syncExHandler = new MockExceptionHandler();
        MockStreamEndpoint writeEndpoint = new MockStreamEndpoint();
        MockStreamResource writeResource = new MockStreamResource("mock1");
        MockWC allWriteResult = new MockWC();
        long appendPos = 0;
        Validator validator;
        KReference<byte[]> ref1, ref2, ref3;
        StreamWrite.Request swr1, swr2, swr3;

        KBlock b1 = new KBlockImpl(0, 0, 2);
        ref1 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr1 = new StreamWrite.Request(writeResource, b1.lowerAbsEndpoint(), ref1.getValue().get());

        KBlock b2 = new KBlockImpl(2, 6, 8);
        ref2 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr2 = new StreamWrite.Request(writeResource, b2.lowerAbsEndpoint(), ref2.getValue().get());

        KBlock b3 = new KBlockImpl(1, 3, 5);
        ref3 = KReferenceFactory.getReference(new byte[]{1, 2, 3});
        swr3 = new StreamWrite.Request(writeResource, b3.lowerAbsEndpoint(), ref3.getValue().get());

        //settig up validators;
        proxy.expect(new PortValidator(MockStreamPort.class, false));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr1));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr3));
        proxy.expect(new EventContentValidator(new StreamWriteReqEC(), swr2));

        //actual run
        SimpleAppendKBuffer sakBuf = new SimpleAppendKBuffer(config, proxy, syncExHandler, writeEndpoint, writeResource, 0);

        //write1
        sakBuf.write(b1, ref1, allWriteResult);
        ref1.release();
        Assert.assertTrue(ref1.isValid());
        //answer to write1
        sakBuf.handleWriteResp.handle(swr1.respond(Result.success(true)));
        Assert.assertFalse(ref1.isValid());
        Assert.assertTrue(sakBuf.isIdle());
        //write2
        sakBuf.write(b2, ref2, allWriteResult);
        ref2.release();
        Assert.assertTrue(ref2.isValid());
        //write3
        sakBuf.write(b3, ref3, allWriteResult);
        ref3.release();
        Assert.assertTrue(ref3.isValid());
        //answer to write3
        sakBuf.handleWriteResp.handle(swr3.respond(Result.externalUnsafeFailure(new IllegalStateException("test failure"))));
        Assert.assertFalse(ref2.isValid());
        Assert.assertFalse(ref3.isValid());
        Assert.assertFalse(sakBuf.isIdle());
        Assert.assertEquals(1, syncExHandler.getExceptionCounter());

        //validation
        validator = proxy.validate();
        if (validator != null) {
            Assert.fail(validator.toString());
        }
    }
}
