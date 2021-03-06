/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceAvailabilityObserver;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnterpriseInstalledPackagesPreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class EnterpriseInstalledPackagesPreferenceControllerTest {

    private static final String KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES
            = "number_enterprise_installed_packages";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    @Mock private PreferenceAvailabilityObserver mObserver;

    private EnterpriseInstalledPackagesPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mController = new EnterpriseInstalledPackagesPreferenceController(mContext,
                null /* lifecycle */, true /* async */);
        mController.setAvailabilityObserver(mObserver);
    }

    @Test
    public void testGetAvailabilityObserver() {
        assertThat(mController.getAvailabilityObserver()).isEqualTo(mObserver);
    }

    private void setNumberOfEnterpriseInstalledPackages(int number, boolean async) {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                ((ApplicationFeatureProvider.NumberOfAppsCallback)
                        invocation.getArguments()[1]).onNumberOfAppsResult(number);
                return null;
            }}).when(mFeatureFactory.applicationFeatureProvider)
                    .calculateNumberOfPolicyInstalledApps(eq(async), anyObject());
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(true);

        setNumberOfEnterpriseInstalledPackages(0, true /* async */);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isFalse();
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES,
                false);

        setNumberOfEnterpriseInstalledPackages(20, true /* async */);
        when(mContext.getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_packages_lower_bound, 20, 20))
                .thenReturn("minimum 20 apps");
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("minimum 20 apps");
        assertThat(preference.isVisible()).isTrue();
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES,
                true);
    }

    @Test
    public void testIsAvailableSync() {
        final EnterpriseInstalledPackagesPreferenceController controller
                = new EnterpriseInstalledPackagesPreferenceController(mContext,
                        null /* lifecycle */, false /* async */);
        controller.setAvailabilityObserver(mObserver);

        setNumberOfEnterpriseInstalledPackages(0, false /* async */);
        assertThat(controller.isAvailable()).isFalse();
        verify(mObserver).onPreferenceAvailabilityUpdated(
                KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES, false);

        setNumberOfEnterpriseInstalledPackages(20, false /* async */);
        assertThat(controller.isAvailable()).isTrue();
        verify(mObserver).onPreferenceAvailabilityUpdated(
                KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES, true);
    }

    @Test
    public void testIsAvailableAsync() {
        setNumberOfEnterpriseInstalledPackages(0, true /* async */);
        assertThat(mController.isAvailable()).isTrue();
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES), anyBoolean());

        setNumberOfEnterpriseInstalledPackages(20, true /* async */);
        assertThat(mController.isAvailable()).isTrue();
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES), anyBoolean());
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey())
                .isEqualTo(KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES);
    }
}
