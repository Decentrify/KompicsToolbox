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
package se.sics.ktoolbox.simulator.instrumentation.decorators.simple;

import com.google.common.base.Optional;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import org.javatuples.Pair;
import org.javatuples.Quartet;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FingerprintFileComparator {

    private final String file1;
    private final String file2;

    public FingerprintFileComparator(String file1, String file2) {
        this.file1 = file1;
        this.file2 = file2;
    }

    public Pair<Optional<Quartet>, Optional<Quartet>> compareFiles() {
        try (
                BufferedReader reader1 = new BufferedReader(new FileReader(file1));
                BufferedReader reader2 = new BufferedReader(new FileReader(file1))) {
            String lineFile1, lineFile2;
            while (true) {
                lineFile1 = reader1.readLine();
                lineFile2 = reader2.readLine();
                if (lineFile1 == null || lineFile2 == null) {
                    break;
                }
                if (!lineFile1.equals(lineFile2)) {
                    StringTokenizer st1 = new StringTokenizer(lineFile1, " ");
                    Quartet q1 = Quartet.with(Long.parseLong(st1.nextToken()),
                            st1.nextToken(), st1.nextToken(), st1.nextToken());
                    StringTokenizer st2 = new StringTokenizer(lineFile2, " ");
                    Quartet q2 = Quartet.with(Long.parseLong(st2.nextToken()),
                            st2.nextToken(), st2.nextToken(), st2.nextToken());
                    return Pair.with(Optional.of(q1), Optional.of(q2));
                }
            }
            if (lineFile1 != null) {
                StringTokenizer st1 = new StringTokenizer(lineFile1, " ");
                Quartet q1 = Quartet.with(Long.parseLong(st1.nextToken()),
                        st1.nextToken(), st1.nextToken(), st1.nextToken());
                Optional<Quartet> q2 = Optional.absent();
                return Pair.with(Optional.of(q1), q2);
            }
            if (lineFile2 != null) {
                Optional<Quartet> q1 = Optional.absent();
                StringTokenizer st2 = new StringTokenizer(lineFile2, " ");
                Quartet q2 = Quartet.with(Long.parseLong(st2.nextToken()),
                        st2.nextToken(), st2.nextToken(), st2.nextToken());
                return Pair.with(q1, Optional.of(q2));
            }
            Optional<Quartet> q = Optional.absent();
            return Pair.with(q, q);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
