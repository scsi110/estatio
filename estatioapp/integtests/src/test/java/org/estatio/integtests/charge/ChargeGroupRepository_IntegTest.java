/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.integtests.charge;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import org.estatio.charge.dom.ChargeGroup;
import org.estatio.charge.dom.ChargeGroupRepository;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.charge.ChargeGroupRefData;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class ChargeGroupRepository_IntegTest extends EstatioIntegrationTest {

    @Inject
    ChargeGroupRepository chargeGroupRepository;

    public static class FindChargeGroup extends ChargeGroupRepository_IntegTest {

        @Before
        public void setupData() {
            runFixtureScript(new EstatioBaseLineFixture());
        }

        @Test
        public void whenExists() throws Exception {
            ChargeGroup chargeGroup = chargeGroupRepository.findChargeGroup(ChargeGroupRefData.REF_RENT);
            assertThat(chargeGroup).isNotNull();
        }

    }

    public static class CreateChargeGroup extends ChargeGroupRepository_IntegTest {

        @Before
        public void setupData() {
            runFixtureScript(new EstatioBaseLineFixture());
        }

        @Test
        public void happyCase() throws Exception {
            final ChargeGroup chargeGroup = chargeGroupRepository.createChargeGroup("TEST CHARGE GROUP", "Test charge group");
            assertThat(chargeGroup.getReference()).isEqualTo("TEST CHARGE GROUP");
        }
    }
}