/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.kb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.kb.model.Entity;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component
public class KnowledgeBaseExtensionRegistryImpl
    implements KnowledgeBaseExtensionRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final List<KnowledgeBaseExtension> extensionsProxy;
    
    private List<KnowledgeBaseExtension> extensions;
        
    public KnowledgeBaseExtensionRegistryImpl(
                @Lazy @Autowired(required = false) List<KnowledgeBaseExtension> aExtensions)
    {
        extensionsProxy = aExtensions;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    /* package private */ void init()
    {
        List<KnowledgeBaseExtension> exts = new ArrayList<>();

        if (extensionsProxy != null) {
            exts.addAll(extensionsProxy);
            OrderComparator.sort(exts);
        
            for (KnowledgeBaseExtension fs : exts) {
                log.info("Found kb extension: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        extensions = Collections.unmodifiableList(exts);
    }
    
    
    @Override
    public List<KnowledgeBaseExtension> getExtensions()
    {
        return extensions;
    }
    
    @Override
    public KnowledgeBaseExtension getExtension(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return extensions.stream().filter(ext -> aId.equals(ext.getBeanName())).findFirst()
                    .orElse(null);
        }
    }
    
    @Override
    public List<Entity> fireDisambiguate(KnowledgeBase aKB, IRI conceptIri, 
            AnnotatorState aState, AnnotationActionHandler aActionHandler) {
        for (KnowledgeBaseExtension ext: getExtensions()) {
            return ext.disambiguate(aKB, conceptIri, aState, aActionHandler);
        }
        throw new IllegalStateException();
    }
    
}
