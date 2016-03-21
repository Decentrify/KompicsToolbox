/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.ktoolbox.simulator.distributions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Random;

/**
 * The <code>BigIntegerExponentialDistribution</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: BigIntegerExponentialDistribution.java 750 2009-04-02 09:55:01Z
 * Cosmin $
 */
public class BigIntegerExponentialDistribution extends Distribution<BigInteger> {

    private static final long serialVersionUID = -917906136112393003L;

    private final Random random;
    private final BigDecimal mean;

    public BigIntegerExponentialDistribution(BigInteger mean, Random random) {
        super(Type.EXPONENTIAL, BigInteger.class);
        this.random = random;
        this.mean = new BigDecimal(mean);
    }

    @Override
    public final BigInteger draw() {
        double u = random.nextDouble();
        BigDecimal d = new BigDecimal(Math.log(1 - u));
        return d.multiply(mean.negate()).round(MathContext.UNLIMITED).toBigInteger();
    }
}
