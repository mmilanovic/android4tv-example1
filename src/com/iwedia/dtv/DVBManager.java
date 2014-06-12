/*
 * Copyright (C) 2014 iWedia S.A. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.iwedia.dtv;

import android.os.RemoteException;

import com.iwedia.activities.DTVActivity;
import com.iwedia.callbacks.EPGCallBack;
import com.iwedia.callbacks.ScanCallBack;
import com.iwedia.dtv.dtvmanager.DTVManager;
import com.iwedia.dtv.dtvmanager.IDTVManager;
import com.iwedia.dtv.epg.EpgEventType;
import com.iwedia.dtv.route.broadcast.IBroadcastRouteControl;
import com.iwedia.dtv.route.broadcast.RouteDemuxDescriptor;
import com.iwedia.dtv.route.broadcast.RouteFrontendDescriptor;
import com.iwedia.dtv.route.broadcast.RouteFrontendType;
import com.iwedia.dtv.route.common.ICommonRouteControl;
import com.iwedia.dtv.route.common.RouteDecoderDescriptor;
import com.iwedia.dtv.route.common.RouteInputOutputDescriptor;
import com.iwedia.dtv.service.IServiceControl;
import com.iwedia.dtv.service.ServiceDescriptor;
import com.iwedia.dtv.service.SourceType;
import com.iwedia.dtv.swupdate.SWVersionType;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.dtv.types.TimeDate;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * DVBManager - Class For Handling MW Components.
 */
public class DVBManager {
    public static final String TAG = "DVBManager";
    /** DTV Service Intent Action. */
    private IDTVManager mDTVManager = null;
    private int mCurrentLiveRoute = -1;
    private int mLiveRouteSat = -1;
    private int mLiveRouteTer = -1;
    private int mLiveRouteCab = -1;
    private int mLiveRouteIp = -1;
    /** EPG Filter ID */
    private int mEPGFilterID = -1;
    /** Currently active list in comedia. */
    private int mCurrentListIndex = 0;
    /** IP stuff */
    private int mCurrentChannelNumberIp = -1;
    private boolean ipAndSomeOtherTunerType = false;
    /** DVB Manager Instance. */
    private static DVBManager sInstance = null;
    /** CallBack for UI. */
    private DVBStatus mDVBStatus = null;
    /** Scan CallBack. */
    private ScanCallBack mScanCallBack = null;
    /** EPG CallBack. */
    private EPGCallBack mEpgCallBack = null;
    /** Volume Mute Status. */
    private boolean mVolumeMute = false;

    /**
     * CallBack for currently DVB status.
     */
    public interface DVBStatus {
        /** Alert UI that channel is scrambled. */
        public void channelIsScrambled(boolean status);

        /** Zapping on Same Channel. */
        public void zappingOnSameCahnnel();

        /** Antenna Connected Status. */
        public void antennaConnected(boolean status);

        /** Update Now Next values. */
        public void updateNowNext();
    }

    public static DVBManager getInstance() throws InternalException {
        if (sInstance == null) {
            sInstance = new DVBManager();
        }
        return sInstance;
    }

    private DVBManager() throws InternalException {
        mDTVManager = new DTVManager();
        InitializeDTVService();
    }

    /**
     * Initialize Service.
     * 
     * @throws InternalException
     */
    private void InitializeDTVService() throws InternalException {
        initializeRouteId();
        mEPGFilterID = mDTVManager.getEpgControl().createEventList();
    }

    /**
     * Initialize Descriptors For Live Route.
     * 
     * @throws RemoteException
     */
    private void initializeRouteId() {
        IBroadcastRouteControl broadcastRouteControl = mDTVManager
                .getBroadcastRouteControl();
        ICommonRouteControl commonRouteControl = mDTVManager
                .getCommonRouteControl();
        /**
         * RETRIEVE DEMUX DESCRIPTOR.
         */
        RouteDemuxDescriptor demuxDescriptor = broadcastRouteControl
                .getDemuxDescriptor(0);
        /**
         * RETRIEVE DECODER DESCRIPTOR.
         */
        RouteDecoderDescriptor decoderDescriptor = commonRouteControl
                .getDecoderDescriptor(0);
        /**
         * RETRIEVING OUTPUT DESCRIPTOR.
         */
        RouteInputOutputDescriptor outputDescriptor = commonRouteControl
                .getInputOutputDescriptor(0);
        /**
         * GET NUMBER OF FRONTENDS.
         */
        int numberOfFrontends = broadcastRouteControl.getFrontendNumber();
        /**
         * FIND DVB and IP front-end descriptors.
         */
        EnumSet<RouteFrontendType> frontendTypes = null;
        for (int i = 0; i < numberOfFrontends; i++) {
            RouteFrontendDescriptor frontendDescriptor = broadcastRouteControl
                    .getFrontendDescriptor(i);
            frontendTypes = frontendDescriptor.getFrontendType();
            for (RouteFrontendType frontendType : frontendTypes) {
                switch (frontendType) {
                    case SAT: {
                        if (mLiveRouteSat == -1) {
                            mLiveRouteSat = getLiveRouteId(frontendDescriptor,
                                    demuxDescriptor, decoderDescriptor,
                                    outputDescriptor, broadcastRouteControl);
                            FrontendManager.frontendFound(new Frontend(
                                    mLiveRouteSat, i));
                        }
                        break;
                    }
                    case CAB: {
                        if (mLiveRouteCab == -1) {
                            mLiveRouteCab = getLiveRouteId(frontendDescriptor,
                                    demuxDescriptor, decoderDescriptor,
                                    outputDescriptor, broadcastRouteControl);
                            FrontendManager.frontendFound(new Frontend(
                                    mLiveRouteCab, i));
                        }
                        break;
                    }
                    case TER: {
                        if (mLiveRouteTer == -1) {
                            mLiveRouteTer = getLiveRouteId(frontendDescriptor,
                                    demuxDescriptor, decoderDescriptor,
                                    outputDescriptor, broadcastRouteControl);
                            FrontendManager.frontendFound(new Frontend(
                                    mLiveRouteTer, i));
                        }
                        break;
                    }
                    case IP: {
                        if (mLiveRouteIp == -1) {
                            mLiveRouteIp = getLiveRouteId(frontendDescriptor,
                                    demuxDescriptor, decoderDescriptor,
                                    outputDescriptor, broadcastRouteControl);
                            FrontendManager.frontendFound(new Frontend(
                                    mLiveRouteIp, i));
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        if (mLiveRouteIp != -1
                && (mLiveRouteCab != -1 || mLiveRouteSat != -1 || mLiveRouteTer != -1)) {
            ipAndSomeOtherTunerType = true;
        }
    }

    /**
     * Get Live Route From Descriptors.
     * 
     * @param fDescriptor
     * @param mDemuxDescriptor
     * @param mDecoderDescriptor
     * @param mOutputDescriptor
     */
    private int getLiveRouteId(RouteFrontendDescriptor fDescriptor,
            RouteDemuxDescriptor mDemuxDescriptor,
            RouteDecoderDescriptor mDecoderDescriptor,
            RouteInputOutputDescriptor mOutputDescriptor,
            IBroadcastRouteControl routeControl) {
        return routeControl.getLiveRoute(fDescriptor.getFrontendId(),
                mDemuxDescriptor.getDemuxId(),
                mDecoderDescriptor.getDecoderId());
    }

    /**
     * Stop MW video playback.
     * 
     * @throws InternalException
     */
    public void stopDTV() throws InternalException {
        mDTVManager.getServiceControl().stopService(mCurrentLiveRoute);
        mDTVManager.getScanControl().unregisterCallback(mScanCallBack);
        mDTVManager.getEpgControl().releaseEventList(mEPGFilterID);
        mDTVManager.getEpgControl().unregisterCallback(mEpgCallBack,
                mEPGFilterID);
        sInstance = null;
    }

    /**
     * Register CallBacks.
     */
    public void registerCallBacks() {
        mScanCallBack = new ScanCallBack();
        mDTVManager.getScanControl().registerCallback(mScanCallBack);
        mEpgCallBack = new EPGCallBack();
        mDTVManager.getEpgControl()
                .registerCallback(mEpgCallBack, mEPGFilterID);
    }

    /**
     * Change Channel Up.
     * 
     * @return Channel Info Object.
     * @throws InternalException
     * @throws IllegalArgumentException
     */
    public ChannelInfo changeChannelUp() throws IllegalArgumentException,
            InternalException {
        int currentChannel = 0;
        try {
            currentChannel = getCurrentChannelNumber();
        } catch (InternalException e) {
            currentChannel = DTVActivity.getLastWatchedChannelIndex();
        }
        int listSize = getChannelListSize();
        if (listSize == 0) {
            return null;
        }
        return changeChannelByNumber((currentChannel + 1) % listSize, false);
    }

    /**
     * Change Channel Down.
     * 
     * @return Channel Info Object.
     * @throws InternalException
     * @throws IllegalArgumentException
     */
    public ChannelInfo changeChannelDown() throws IllegalArgumentException,
            InternalException {
        int currentChannelNumber = 0;
        try {
            currentChannelNumber = getCurrentChannelNumber();
        } catch (InternalException e) {
            currentChannelNumber = DTVActivity.getLastWatchedChannelIndex();
        }
        int listSize = getChannelListSize();
        if (listSize == 0) {
            return null;
        }
        return changeChannelByNumber((--currentChannelNumber + listSize)
                % listSize, false);
    }

    /**
     * Change Channel by Number.
     * 
     * @return Channel Info Object or null if error occurred.
     * @throws IllegalArgumentException
     * @throws InternalException
     */
    public ChannelInfo changeChannelByNumber(int channelNumber, boolean initial)
            throws InternalException {
        int listSize = getChannelListSize();
        if (listSize == 0) {
            return null;
        }
        channelNumber = (channelNumber + listSize) % listSize;
        int currentChannelNumber = 0;
        try {
            currentChannelNumber = getCurrentChannelNumber();
        } catch (InternalException e) {
            currentChannelNumber = DTVActivity.getLastWatchedChannelIndex();
        }
        if (channelNumber == currentChannelNumber && !initial) {
            mDVBStatus.zappingOnSameCahnnel();
            return null;
        } else {
            int numberOfDtvChannels = listSize
                    - (mLiveRouteIp == -1 ? 0 : DTVActivity.sIpChannels.size());
            /** For regular DVB channel. */
            if (channelNumber < numberOfDtvChannels) {
                ServiceDescriptor desiredService = mDTVManager
                        .getServiceControl().getServiceDescriptor(
                                mCurrentListIndex,
                                ipAndSomeOtherTunerType ? channelNumber + 1
                                        : channelNumber);
                /** Channel is Scrambled Toast. Currently Disabled. */
                // mDVBStatus.channelIsScrambled(desiredService.isScrambled());
                int route = getActiveRouteByServiceType(desiredService
                        .getSourceType());
                if (route == -1) {
                    return null;
                }
                mCurrentLiveRoute = route;
                mDTVManager.getServiceControl().startService(
                        route,
                        mCurrentListIndex,
                        ipAndSomeOtherTunerType ? channelNumber + 1
                                : channelNumber);
            }
            /** For IP. */
            else {
                mCurrentLiveRoute = mLiveRouteIp;
                mCurrentChannelNumberIp = channelNumber;
                mDTVManager.getServiceControl().zapURL(
                        mLiveRouteIp,
                        DTVActivity.sIpChannels.get(
                                channelNumber - numberOfDtvChannels).getUrl());
            }
            mDVBStatus.antennaConnected(FrontendManager
                    .getAntennaState(mCurrentLiveRoute));
            DTVActivity.setLastWatchedChannelIndex(channelNumber);
            return getChannelInfo(channelNumber, true);
        }
    }

    /**
     * Return route by service type.
     * 
     * @param serviceType
     *        Service type to check.
     * @return Desired route, or 0 if service type is undefined.
     */
    private int getActiveRouteByServiceType(SourceType sourceType) {
        switch (sourceType) {
            case CAB: {
                return mLiveRouteCab;
            }
            case TER: {
                return mLiveRouteTer;
            }
            case SAT: {
                return mLiveRouteSat;
            }
            case IP: {
                return mLiveRouteIp;
            }
            default:
                return -1;
        }
    }

    /**
     * Get Size of Channel List.
     */
    public int getChannelListSize() {
        int serviceCount = mDTVManager.getServiceControl().getServiceListCount(
                mCurrentListIndex);
        if (ipAndSomeOtherTunerType) {
            serviceCount += DTVActivity.sIpChannels.size();
            serviceCount--;
        } else
        /** Only IP. */
        if (mLiveRouteIp != -1) {
            serviceCount = DTVActivity.sIpChannels.size();
        }
        return serviceCount;
    }

    /**
     * Get Channel Names.
     */
    public ArrayList<String> getChannelNames() {
        ArrayList<String> channelNames = new ArrayList<String>();
        String channelName = "";
        int channelListSize = getChannelListSize()
                - (mLiveRouteIp == -1 ? 0 : DTVActivity.sIpChannels.size());
        IServiceControl serviceControl = mDTVManager.getServiceControl();
        /** If there is IP first element in service list is DUMMY */
        channelListSize = ipAndSomeOtherTunerType ? channelListSize + 1
                : channelListSize;
        for (int i = ipAndSomeOtherTunerType ? 1 : 0; i < channelListSize; i++) {
            channelName = serviceControl.getServiceDescriptor(
                    mCurrentListIndex, i).getName();
            channelNames.add(channelName);
        }
        /** Add IP. */
        if (mLiveRouteIp != -1) {
            for (int i = 0; i < DTVActivity.sIpChannels.size(); i++) {
                channelNames.add(DTVActivity.sIpChannels.get(i).getName());
            }
        }
        return channelNames;
    }

    /**
     * Get Current Channel Number.
     */
    public int getCurrentChannelNumber() throws InternalException {
        /** For IP */
        if (mCurrentLiveRoute == mLiveRouteIp) {
            return mCurrentChannelNumberIp;
        }
        int current = (mDTVManager.getServiceControl().getActiveService(
                mCurrentLiveRoute).getServiceIndex());
        current = current - (ipAndSomeOtherTunerType ? 1 : 0);
        /** This is error in comedia and should be ignored. */
        if (current < 0) {
            throw new InternalException();
        }
        return current;
    }

    /**
     * Get Current Channel Number and Channel Name.
     * 
     * @return Object of Channel Info class.
     * @throws IllegalArgumentException
     */
    public ChannelInfo getChannelInfo(int channelNumber, boolean channelChange) {
        if (channelNumber < 0 || channelNumber >= getChannelListSize()) {
            return null;
        }
        int numberOfDtvChannels = getChannelListSize()
                - (mLiveRouteIp == -1 ? 0 : DTVActivity.sIpChannels.size());
        /** Return DTV channel. */
        if (channelNumber < numberOfDtvChannels) {
            String channelName = mDTVManager
                    .getServiceControl()
                    .getServiceDescriptor(
                            mCurrentListIndex,
                            ipAndSomeOtherTunerType ? channelNumber + 1
                                    : channelNumber).getName();
            if (channelChange) {
                return new ChannelInfo(channelNumber + 1, channelName, null,
                        null);
            } else {
                return new ChannelInfo(channelNumber + 1, channelName,
                        mDTVManager.getEpgControl().getPresentFollowingEvent(
                                mEPGFilterID, channelNumber,
                                EpgEventType.PRESENT_EVENT), mDTVManager
                                .getEpgControl().getPresentFollowingEvent(
                                        mEPGFilterID, channelNumber,
                                        EpgEventType.FOLLOWING_EVENT));
            }
        }
        /** Return IP channel. */
        else {
            return new ChannelInfo(channelNumber + 1, DTVActivity.sIpChannels
                    .get(channelNumber - numberOfDtvChannels).getName(), null,
                    null);
        }
    }

    /**
     * Set Current Volume.
     */
    public void setVolume(int volume) {
        mDTVManager.getAudioControl().setVolume(mCurrentLiveRoute,
                (double) volume);
    }

    /**
     * Get Current Volume.
     */
    public int getCurrentVolume() {
        return (int) mDTVManager.getAudioControl().getVolume(mCurrentLiveRoute);
    }

    /**
     * Set Volume Mute Status.
     */
    public int setVolumeMute() {
        mVolumeMute = !mVolumeMute;
        mDTVManager.getAudioControl().muteAudio(mCurrentLiveRoute, mVolumeMute);
        return mVolumeMute ? 0 : getCurrentVolume();
    }

    /**
     * Get Current Time and Date.
     */
    public TimeDate getCurrentTimeDate() {
        return mDTVManager.getSetupControl().getTimeDate();
    }

    /**
     * Show Layout for Antenna Connected.
     */
    public void showAntennaConnectedLayout(boolean status, int frontendIndex) {
        FrontendManager.setAntennaState(frontendIndex, status);
        /**
         * Send callback if antenna is disconnected on live route, or it is
         * connected
         */
        int routeId = FrontendManager
                .getLiveRouteByFrontendIndex(frontendIndex);
        if ((!status && routeId == mCurrentLiveRoute) || status) {
            mDVBStatus.antennaConnected(status);
        }
    }

    public String getSwVersion(SWVersionType type) {
        return mDTVManager.getSoftwareUpdateControl().getSWVersion(type);
    }

    /**
     * Update Now Next.
     */
    public void udpateNowNext() {
        mDVBStatus.updateNowNext();
    }

    /**
     * Set DVB Status Instance.
     */
    public void setDVBStatus(DVBStatus dvbStatus) {
        mDVBStatus = dvbStatus;
    }

    public boolean isIpAndSomeOtherTunerType() {
        return ipAndSomeOtherTunerType;
    }
}
