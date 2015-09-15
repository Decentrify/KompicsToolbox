///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.aggregator.server.other;
//
//import se.sics.ktoolbox.aggregator.server.util.DesignInfo;
//import se.sics.ktoolbox.aggregator.server.util.DesignInfoContainer;
//
///**
// * Response Matcher for a simple window processing response.
// *
// * Created by babbarshaer on 2015-09-05.
// */
//public abstract class ProcessingResponseMatcher<DI_O extends DesignInfo> implements ResponseMatcher<DesignInfoContainer<DI_O>> {
//
//    public DesignInfoContainer<DI_O> container;
//
//    public ProcessingResponseMatcher(DesignInfoContainer<DI_O> container){
//        this.container = container;
//    }
//
//    public DesignInfoContainer<DI_O> getContent() {
//        return this.container;
//    }
//
//    public Class<DesignInfoContainer<DI_O>> extractPattern() {
//        return (Class<DesignInfoContainer<DI_O>>)this.container.getClass();
//    }
//
//    public DesignInfoContainer<DI_O> extractValue() {
//        return this.container;
//    }
//}