<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

namespace Application\Factory;

use Zend\ServiceManager\ServiceLocatorInterface;
use Zend\ServiceManager\FactoryInterface;
use Application\Repository\PluginRepository;
use Application\Repository\PluginVersionRepository;
use Application\Repository\NbVersionRepository;
use Application\Repository\NbVersionPluginVersionRepository;
use Application\Controller\PluginVersionController;
use Application\Repository\VerificationRepository;

class pluginVersionControllerFactory implements FactoryInterface
{
    public function createService(ServiceLocatorInterface $serviceLocator) {
        $em = $serviceLocator->getServiceLocator()->get('Doctrine\ORM\EntityManager');
        
        $repository = new PluginRepository();
        $repository->setEntityManager($em);
        $pvRepository = new PluginVersionRepository();
        $pvRepository->setEntityManager($em);
        $nbvRepository = new NbVersionRepository();
        $nbvRepository->setEntityManager($em);        
        $nbVersionPluginVersionRepo = new NbVersionPluginVersionRepository();
        $nbVersionPluginVersionRepo->setEntityManager($em);
        $config = $serviceLocator->getServiceLocator()->get('config');
        $vrepository = new VerificationRepository();
        $vrepository->setEntityManager($em);


        return new PluginVersionController($repository, $pvRepository, $nbvRepository, $nbVersionPluginVersionRepo, $config, $vrepository);
    }
}
