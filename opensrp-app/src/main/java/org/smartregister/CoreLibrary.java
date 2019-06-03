package org.smartregister;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.smartregister.authorizer.P2PSyncAuthorizationService;
import org.smartregister.p2p.P2PLibrary;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.P2PReceiverTransferDao;
import org.smartregister.repository.P2PSenderTransferDao;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.P2PSyncFinishCallback;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by keyman on 31/07/17.
 */
public class CoreLibrary {
    private final Context context;

    private static CoreLibrary instance;

    private final SyncConfiguration syncConfiguration;
    private static long buildTimeStamp;

    private String ecClientFieldsFile = "ec_client_fields.json";

    private P2POptions p2POptions;


    public static void init(Context context) {
        init(context, null);
    }

    public static void init(Context context, SyncConfiguration syncConfiguration) {
        if (instance == null) {
            instance = new CoreLibrary(context, syncConfiguration, null);
        }
    }

    public static void init(Context context, SyncConfiguration syncConfiguration, long buildTimestamp) {
        if (instance == null) {
            instance = new CoreLibrary(context, syncConfiguration, null);
            buildTimeStamp = buildTimestamp;
        }
    }

    public static void init(Context context, SyncConfiguration syncConfiguration, long buildTimestamp, @NonNull P2POptions options) {
        if (instance == null) {
            instance = new CoreLibrary(context, syncConfiguration, options);
            buildTimeStamp = buildTimestamp;
        }
    }

    public static CoreLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException(" Instance does not exist!!! Call "
                    + CoreLibrary.class.getName()
                    + ".init method in the onCreate method of "
                    + "your Application class ");
        }
        return instance;
    }

    private CoreLibrary(Context contextArg, SyncConfiguration syncConfiguration, @Nullable P2POptions p2POptions) {
        context = contextArg;
        this.syncConfiguration = syncConfiguration;
        this.p2POptions = p2POptions;

        initP2pLibrary(null);
    }

    public void initP2pLibrary(@Nullable String username) {
        if (p2POptions != null && p2POptions.isEnableP2PLibrary()) {
            String p2pUsername = username;
            AllSharedPreferences allSharedPreferences = new AllSharedPreferences(getDefaultSharedPreferences(context.applicationContext()));
            if (p2pUsername == null) {
                p2pUsername = allSharedPreferences.fetchRegisteredANM();
            }

            if (!TextUtils.isEmpty(p2pUsername)) {
                String teamId = allSharedPreferences.fetchDefaultTeamId(p2pUsername);

                if (p2POptions.getAuthorizationService() == null) {
                    p2POptions.setAuthorizationService(new P2PSyncAuthorizationService(teamId));
                }

                if (p2POptions.getReceiverTransferDao() == null) {
                    p2POptions.setReceiverTransferDao(new P2PReceiverTransferDao());
                }

                if (p2POptions.getSenderTransferDao() == null) {
                    p2POptions.setSenderTransferDao(new P2PSenderTransferDao());
                }

                if (p2POptions.getClientProcessor() == null) {
                    p2POptions.setClientProcessor(ClientProcessorForJava.getInstance(context.applicationContext()));
                }

                if (p2POptions.getSyncFinishedCallback() == null) {
                    p2POptions.setSyncFinishedCallback(new P2PSyncFinishCallback());
                }

                P2PLibrary.Options options = new P2PLibrary.Options(context.applicationContext()
                        , teamId, p2pUsername, p2POptions.getAuthorizationService(), p2POptions.getReceiverTransferDao()
                        , p2POptions.getSenderTransferDao());
                options.setBatchSize(p2POptions.getBatchSize());
                options.setSyncFinishedCallback(p2POptions.getSyncFinishedCallback());

                P2PLibrary.init(options);
            }
        }
    }

    public Context context() {
        return context;
    }

    /**
     * Use this method when testing.
     * It should replace org.smartregister.Context#setInstance(org.smartregister.Context) which has been removed
     *
     * @param context
     */
    public static void reset(Context context) {
        if (context != null) {
            instance = new CoreLibrary(context, null, null);
        }
    }

    public static void reset(Context context, SyncConfiguration syncConfiguration) {
        if (context != null) {
            instance = new CoreLibrary(context, syncConfiguration, null);
        }
    }

    public SyncConfiguration getSyncConfiguration() {
        if (syncConfiguration == null) {
            throw new IllegalStateException(" Instance does not exist!!! Call "
                    + CoreLibrary.class.getName()
                    + ".init method in the onCreate method of "
                    + "your Application class ");
        }
        return syncConfiguration;
    }

    public static long getBuildTimeStamp() {
        return buildTimeStamp;
    }

    public String getEcClientFieldsFile() {
        return ecClientFieldsFile;
    }

    public void setEcClientFieldsFile(String ecClientFieldsFile) {
        this.ecClientFieldsFile = ecClientFieldsFile;
    }

    @Nullable
    public P2POptions getP2POptions() {
        return p2POptions;
    }

}
