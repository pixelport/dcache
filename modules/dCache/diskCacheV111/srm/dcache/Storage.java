// $Id$

/*
COPYRIGHT STATUS:
  Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
  software are sponsored by the U.S. Department of Energy under Contract No.
  DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
  non-exclusive, royalty-free license to publish or reproduce these documents
  and software for U.S. Government purposes.  All documents and software
  available from this server are protected under the U.S. and Foreign
  Copyright Laws, and FNAL reserves all rights.


 Distribution of the software available from this server is free of
 charge subject to the user following the terms of the Fermitools
 Software Legal Information.

 Redistribution and/or modification of the software shall be accompanied
 by the Fermitools Software Legal Information  (including the copyright
 notice).

 The user is asked to feed back problems, benefits, and/or suggestions
 about the software to the Fermilab Software Providers.


 Neither the name of Fermilab, the  URA, nor the names of the contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.



  DISCLAIMER OF LIABILITY (BSD):

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
  OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
  FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
  OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.


  Liabilities of the Government:

  This software is provided by URA, independent from its Prime Contract
  with the U.S. Department of Energy. URA is acting independently from
  the Government and in its own private capacity and is not acting on
  behalf of the U.S. Government, nor as its contractor nor its agent.
  Correspondingly, it is understood and agreed that the U.S. Government
  has no connection to this software and in no manner whatsoever shall
  be liable for nor assume any responsibility or obligation for any claim,
  cost, or damages arising out of or resulting from the use of the software
  available from this server.


  Export Control:

  All documents and software available from this server are subject to U.S.
  export control laws.  Anyone downloading information from this server is
  obligated to secure any necessary Government licenses before exporting
  documents or software obtained from this server.
 */



package diskCacheV111.srm.dcache;

import diskCacheV111.poolManager.PoolSelectionUnit.DirectionType;
import dmg.cells.nucleus.CellAdapter;
import org.dcache.cells.AbstractCell;
import dmg.cells.nucleus.CellPath;
import dmg.cells.nucleus.CellMessage;
import dmg.cells.nucleus.ExceptionEvent;
import dmg.cells.nucleus.CellMessageAnswerable;
import dmg.cells.nucleus.CellVersion;
import dmg.cells.nucleus.NoRouteToCellException;
import org.dcache.cells.CellStub;

import dmg.util.Args;

import dmg.cells.services.login.LoginBrokerInfo;

import diskCacheV111.util.PnfsHandler;
import diskCacheV111.util.PnfsId;
import diskCacheV111.util.FsPath;

import diskCacheV111.vehicles.PnfsGetStorageInfoMessage;

import diskCacheV111.vehicles.PnfsGetFileMetaDataMessage;
import diskCacheV111.vehicles.StorageInfo;
import diskCacheV111.vehicles.PoolMgrQueryPoolsMsg;
import diskCacheV111.vehicles.PnfsSetFileMetaDataMessage;
import diskCacheV111.vehicles.PnfsGetCacheLocationsMessage;
import org.dcache.auth.AuthorizationRecord;
import org.dcache.srm.SrmCancelUseOfSpaceCallbacks;
import org.dcache.srm.SrmReleaseSpaceCallbacks;
import org.dcache.srm.SrmUseSpaceCallbacks;

import org.dcache.srm.util.Configuration;
import org.dcache.srm.security.SslGsiSocketFactory;
import diskCacheV111.util.CacheException;
import diskCacheV111.vehicles.PnfsDeleteEntryMessage;
import diskCacheV111.vehicles.PnfsCreateDirectoryMessage;
import diskCacheV111.vehicles.PnfsRenameMessage;
import diskCacheV111.vehicles.transferManager.
    RemoteGsiftpTransferManagerMessage;
import diskCacheV111.vehicles.transferManager.
    RemoteGsiftpDelegateUserCredentialsMessage;
import diskCacheV111.vehicles.transferManager.TransferManagerMessage;
import diskCacheV111.vehicles.transferManager.CancelTransferMessage;
import diskCacheV111.vehicles.transferManager.TransferCompleteMessage;
import diskCacheV111.vehicles.transferManager.TransferFailedMessage;
import diskCacheV111.vehicles.CopyManagerMessage;
import diskCacheV111.vehicles.PnfsFlagMessage;
import diskCacheV111.vehicles.PoolManagerGetPoolListMessage;
import diskCacheV111.vehicles.PinManagerExtendLifetimeMessage;
import diskCacheV111.services.space.message.ExtendLifetime;
import diskCacheV111.services.space.message.GetFileSpaceTokensMessage;
import diskCacheV111.pools.PoolCellInfo;
import diskCacheV111.pools.PoolCostInfo;
import org.globus.util.GlobusURL;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;

import java.util.Random;
import java.util.Vector;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Collection;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import diskCacheV111.services.PermissionHandler;
import org.dcache.srm.AbstractStorageElement;
import org.dcache.srm.SRMException;
import org.dcache.srm.SRMDuplicationException;
import org.dcache.srm.SRMInternalErrorException;
import org.dcache.srm.SRMInvalidPathException;
import org.dcache.srm.SRMAuthorizationException;
import org.dcache.srm.FileMetaData;
import org.dcache.srm.GetFileInfoCallbacks;
import org.dcache.srm.PrepareToPutCallbacks;
import org.dcache.srm.ReleaseSpaceCallbacks;
import org.dcache.srm.ReserveSpaceCallbacks;
import org.dcache.srm.PrepareToPutInSpaceCallbacks;
import org.dcache.srm.SrmReserveSpaceCallbacks;
import org.dcache.srm.SRMUser;
import diskCacheV111.srm.StorageElementInfo;
import org.dcache.srm.AdvisoryDeleteCallbacks;
import org.dcache.srm.RemoveFileCallbacks;
import org.dcache.srm.PinCallbacks;
import org.dcache.srm.UnpinCallbacks;
import org.dcache.srm.util.Permissions;
import org.dcache.srm.CopyCallbacks;
import org.dcache.srm.scheduler.Job;
import org.dcache.srm.SRM;
import org.dcache.srm.BadSRMObjectException;
import org.dcache.srm.v2_2.TMetaDataSpace;
import org.dcache.srm.v2_2.TRetentionPolicyInfo;
import org.dcache.srm.v2_2.TRetentionPolicy;
import org.dcache.srm.v2_2.TAccessLatency;
import org.dcache.srm.v2_2.TStatusCode;
import org.dcache.srm.v2_2.TReturnStatus;
import org.dcache.util.Checksum;
import org.dcache.util.ChecksumType;
import org.dcache.util.list.DirectoryListSource;
import org.dcache.util.list.DirectoryListPrinter;
import org.dcache.util.list.DirectoryEntry;
import org.dcache.util.list.ListDirectoryHandler;
import diskCacheV111.util.RetentionPolicy;
import diskCacheV111.util.AccessLatency;
import diskCacheV111.services.space.message.GetSpaceMetaData;
import diskCacheV111.services.space.message.GetSpaceTokens;
import diskCacheV111.util.TimeoutCacheException;
import diskCacheV111.util.NotInTrashCacheException;
import diskCacheV111.util.FileNotFoundCacheException;
import diskCacheV111.util.FileExistsCacheException;
import diskCacheV111.util.NotDirCacheException;
import diskCacheV111.util.PermissionDeniedCacheException;
import diskCacheV111.util.FileExistsCacheException;
import org.dcache.auth.persistence.AuthRecordPersistenceManager;
import org.dcache.auth.Subjects;
import org.dcache.commons.stats.RequestCounters;
import org.dcache.vehicles.FileAttributes;
import org.dcache.namespace.FileAttribute;
import org.dcache.namespace.FileType;

import org.ietf.jgss.GSSCredential;


import org.dcache.srm.SRMUser;
import org.dcache.srm.request.RequestCredential;

import java.io.File;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.Subject;

import org.apache.log4j.Logger;

/**
 * SRMCell is the key class that performes communication
 * between SRM server and dcache
 * SRMCell can be run in a separate domain, on
 * a machine that has no knowledge about pnfs
 *
 * @author Timur Perelmutov
 * @author FNAL,CD/ISD
 * @version 	0.9, 20 June 2002
 */

public class Storage
        extends AbstractCell
        implements AbstractStorageElement, Runnable
{
    private final static Logger _log = Logger.getLogger(Storage.class);

    private static boolean kludgeDomainMainWasRun = false;

    /* these are the  protocols
     * that are not sutable for either put or get */
    private static final String[] SRM_PUT_NOT_SUPPORTED_PROTOCOLS
        = { "http" };
    private static final String[] SRM_GET_NOT_SUPPORTED_PROTOCOLS
        = {};
    private static final String[] SRM_PREFERED_PROTOCOLS
        = { "gsiftp", "gsidcap" };

    private String srm_root = "";
    private Args           _args;
    private String _poolManagerName;
    private String _pnfsManagerName;
    private CellPath _poolMgrPath;
    private CellStub _pnfsStub;
    private PnfsHandler _pnfs;
    private PermissionHandler permissionHandler;
    String _hosts[];
    private int __pnfsTimeout = 60 ;
    private String loginBrokerName="LoginBroker";
    private CellPath loginBrokerPath;
    private SRM srm;
    private int __poolManagerTimeout = 60;
    private String remoteGridftpTransferManagerName =
        "RemoteGsiftpTransferManager";
    private final Configuration config = new Configuration();
    private Thread storageInfoUpdateThread;
    private static SRM srmInstance = null;
    private static final Object syncObj = new Object();
    private boolean ignoreClientProtocolOrder; //falseByDefault
    private boolean customGetHostByAddr; //falseByDefault

    private final FsPath _xrootdRootPath;
    private final FsPath _httpRootPath;

    private LoginBrokerHandler _loginBrokerHandler = null ;

    private final DirectoryListSource _listSource;

    // public static SRM getSRMInstance(String xmlConfigPath)
    public static SRM getSRMInstance(final String[] dCacheParams,
            long timeout)
            throws BadSRMObjectException {

        _log.info("Here are the params/args to go to dCache: " + Arrays.toString(dCacheParams));

        _log.debug("entering Storage.getSRMInstance");
        if (srmInstance != null) {
            _log.debug("in Storage.getSRMInstance(), about to " +
                       "return existing srmInstance");
            return srmInstance;
        }
	else {
            // TODO:  Here is the kludge to keep from calling Domain.main
            //        twice, and therefore trying to create 2 instances
            //        of SRM.  We need a better solution than this...

            if (!kludgeDomainMainWasRun) {

                _log.debug(
                        "in Storage.getSRMInstance(),  " +
                        "srmInstance is null, " +
                        "about to call Domain.main()");
                new Thread() {

                    public void run() {

                        // Calling the main method and passing some
                        // arguments is kludgey.  But, we have no other
                        // way of calling Domain; we
                        // cannot modify anything in cells.
                        // ToDo:  Work with DESY to improve class Domain
                        // so that we can activate it without just
                        // calling the main.
                        dmg.cells.services.Domain.main(dCacheParams);
                    }
                }.start();

                _log.debug(
                    "in Storage.getSRMInstance(), " +
                    "started thread that will call " +
                    " Domain.main()");
            } else {
                _log.debug(
                        "in Storage.getSRMInstance(), Domain.main has " +
                        "already been run.");
            }
        }
        long time_expired = 0;
        long wait_period = 1000;
        synchronized (syncObj) {
            while(srmInstance == null ) {
                _log.info("Waiting for srm initialization to complete.");
                try {
                    syncObj.wait(wait_period);
                    time_expired += wait_period;
                } catch (InterruptedException ie) {
                    throw new BadSRMObjectException("Failed to get srmInstance", ie);
                }
                if(time_expired > timeout) {
                    throw new BadSRMObjectException(
                            "startup takes longer then timeout");
                }
            }
        }
        _log.debug("about to return the instance of srm");
        return srmInstance;
    }



    /**
     * Creates the instance of the CRMCell
     *
     * @param  name
     *         The Name of this cell
     * @param  argString
     *         arguments
     */

    public Storage(final String name, String argString) throws Exception {

        super(name , argString );

        _log.debug("In Storage constructor, back from super constructor.");
        _log.info("Starting SRM cell named " + name);

        _args      = getArgs() ;

        __pnfsTimeout =getIntOption("pnfs-timeout",__pnfsTimeout);
        _poolManagerName = getOption("poolManager", "PoolManager" );
        _pnfsManagerName = getOption("pnfsManager" , "PnfsManager") ;
        _poolMgrPath     = new CellPath( _poolManagerName ) ;
        _pnfsStub = new CellStub();
        _pnfsStub.setDestination(_pnfsManagerName);
        _pnfsStub.setCellEndpoint(this);
        _pnfsStub.setTimeout(__pnfsTimeout * 1000);
        _pnfsStub.setRetryOnNoRouteToCell(true);
        _pnfs = new PnfsHandler(_pnfsStub);
        permissionHandler =
            new PermissionHandler(this, new CellPath(_pnfsManagerName));
        InetAddress[] addresses = InetAddress.getAllByName(
                InetAddress.getLocalHost().getHostName());

        _hosts = new String[addresses.length];

        _httpRootPath = new FsPath(getOption("httpRootPath", "/"));
        _xrootdRootPath = new FsPath(getOption("xrootdRootPath", "/"));

        _listSource = new ListDirectoryHandler(_pnfs);
        addMessageListener(_listSource);

        /**
         *  Add addresses ensuring preferred ordering: external addresses are before any
         *  internal interface addresses.
         */
        int nextExternalIfIndex = 0;
        int nextInternalIfIndex = addresses.length-1;

        for( int i = 0; i < addresses.length; i++) {
    		InetAddress addr = addresses[i];

        	if( !addr.isLinkLocalAddress() && !addr.isLoopbackAddress() &&
        			!addr.isSiteLocalAddress() && !addr.isMulticastAddress()) {
        		_hosts [nextExternalIfIndex++] = addr.getHostName();
        	} else {
        		_hosts [nextInternalIfIndex--] = addr.getHostName();
        	}
        }

        String tmpstr = _args.getOpt("config");
        if(tmpstr != null) {
            try {
                config.read(_args.getOpt("config"));
            } catch (Exception e) {
                _log.fatal("can't read config from file: "+_args.getOpt("config"));
                throw e;
            }
        }
        __poolManagerTimeout =getIntOption("pool-manager-timeout",
            __poolManagerTimeout);

        config.setPort(getIntOption("srmport",config.getPort()));
        config.setSizeOfSingleRemoveBatch(getIntOption("size-of-single-remove-batch",config.getSizeOfSingleRemoveBatch()));
        config.setAsynchronousLs(isOptionSetToTrueOrYes("use-asynchronous-ls",config.isAsynchronousLs()));
	config.setGlue_mapfile(getOption("srmmap",config.getGlue_mapfile()));

        config.setMaxNumberOfLsEntries(getIntOption("max-number-of-ls-entries",config.getMaxNumberOfLsEntries()));
        config.setMaxNumberOfLsLevels(getIntOption("max-number-of-ls-levels",config.getMaxNumberOfLsLevels()));

        config.setKpwdfile( getOption("kpwd-file",config.getKpwdfile()) );
        config.setUseGplazmaAuthzCellFlag(isOptionSetToTrueOrYes(
            "use-gplazma-authorization-cell",
            config.getUseGplazmaAuthzCellFlag()));

        config.setDelegateToGplazmaFlag(isOptionSetToTrueOrYes(
            "delegate-to-gplazma",
            config.getDelegateToGplazmaFlag()));

        config.setUseGplazmaAuthzModuleFlag(isOptionSetToTrueOrYes(
            "use-gplazma-authorization-module",
            config.getUseGplazmaAuthzModuleFlag()));

        config.setAuthzCacheLifetime( getOption("srm-authz-cache-lifetime",
            config.getAuthzCacheLifetime()) );
        config.setGplazmaPolicy(getOption("gplazma-authorization-module-policy",
            config.getGplazmaPolicy()) );

        srm_root = getOption("pnfs-srm-path",srm_root);
        config.setSrm_root(srm_root);

        config.setProxies_directory(getOption("proxies-directory",
            config.getProxies_directory()) );

        config.setUrlcopy(getOption("url-copy-command",
            config.getUrlcopy()));

        config.setTimeout_script(getOption("timeout-command",
            config.getTimeout_script()));

        config.setTimeout(getIntOption("timout",config.getTimeout()));

        config.setBuffer_size(getIntOption("buffer_size",
            config.getBuffer_size()));

        config.setTcp_buffer_size(getIntOption("tcp_buffer_size",
            config.getTcp_buffer_size()));

        config.setParallel_streams(getIntOption("parallel_streams",
            config.getParallel_streams()));

        config.setSrmhost(getOption("srmhost",config.getSrmhost()));

        config.setStart_server(isOptionSetToTrueOrYes("start_server",
            config.isStart_server()) );

        config.setDebug(isOptionSetToTrueOrYes("debug", config.isDebug()));
        tmpstr =  _args.getOpt("usekftp");
        if(tmpstr != null && tmpstr.equalsIgnoreCase("true")) {
            config.setGsiftpclinet("kftp");
        } else {
            config.setGsiftpclinet("globus-url-copy");
        }

        config.setUseUrlcopyScript(isOptionSetToTrueOrYes("use-urlcopy-script",
            config.isUseUrlcopyScript()));

        config.setUseDcapForSrmCopy(
            isOptionSetToTrueOrYes("use-dcap-for-srm-copy",
            config.isUseDcapForSrmCopy()));

        config.setUseGsiftpForSrmCopy(
            isOptionSetToTrueOrYes("use-gsiftp-for-srm-copy",
            config.isUseGsiftpForSrmCopy()));

        config.setUseHttpForSrmCopy(isOptionSetToTrueOrYes("use-http-for-srm-copy",
            config.isUseHttpForSrmCopy()));

        config.setUseFtpForSrmCopy(isOptionSetToTrueOrYes("use-ftp-for-srm-copy",
            config.isUseFtpForSrmCopy()));

        config.setGetLifetime(getLongOption(
            "get-lifetime",config.getGetLifetime()));
        config.setBringOnlineLifetime(getLongOption(
            "bring-online-lifetime",config.getBringOnlineLifetime()));
        config.setLsLifetime(getLongOption(
                                     "ls-request-lifetime",config.getLsLifetime()));
        config.setPutLifetime(getLongOption(
            "put-lifetime",config.getPutLifetime()));
        config.setCopyLifetime(getLongOption("copy-lifetime",
            config.getCopyLifetime()));

        config.setRecursiveDirectoryCreation(isOptionSetToTrueOrYes(
            "recursive-dirs-creation",config.isRecursiveDirectoryCreation()));

        config.setAdvisoryDelete(isOptionSetToTrueOrYes("advisory-delete",
            config.isAdvisoryDelete()));

        config.setRemoveFile(isOptionSetToTrueOrYes("rm",config.isRemoveFile()));

        config.setRemoveDirectory(isOptionSetToTrueOrYes("rmdir",
            config.isRemoveDirectory()));

        config.setCreateDirectory(isOptionSetToTrueOrYes("mkdir",
            config.isCreateDirectory()));

        config.setMoveEntry(isOptionSetToTrueOrYes("mv",config.isMoveEntry()));

        config.setSaveMemory(isOptionSetToTrueOrYes("save-memory",
            config.isSaveMemory()));

        config.setReserve_space_implicitely(isOptionSetToTrueOrYes(
            "reserve-space-implicitly",config.isReserve_space_implicitely()));

        config.setSpace_reservation_strict(isOptionSetToTrueOrYes(
            "space-reservation-strict",config.isSpace_reservation_strict()));


        config.setGetPriorityPolicyPlugin(getOption("get-priority-policy",
            config.getGetPriorityPolicyPlugin()));
        config.setBringOnlinePriorityPolicyPlugin(getOption("bring-online-priority-policy",
            config.getBringOnlinePriorityPolicyPlugin()));
        config.setLsPriorityPolicyPlugin(getOption("ls-request-priority-policy",
                                                   config.getLsPriorityPolicyPlugin()));
        config.setPutPriorityPolicyPlugin(getOption("put-priority-policy",
            config.getPutPriorityPolicyPlugin()));
        config.setCopyPriorityPolicyPlugin(getOption("copy-priority-policy",
            config.getCopyPriorityPolicyPlugin()));

        String jdbcPass = _args.getOpt("dbPass");
        String jdbcPwdfile = _args.getOpt("pgPass");
        if((jdbcPass==null && jdbcPwdfile==null)) {
            String error = "database parameters are not specified; use options " +
                "-jdbcUrl, -jdbcDriver, -dbUser and -dbPass/-pgPass";
            _log.fatal(error);
            throw new Exception(error);
        }
        config.setJdbcUrl(getOption("jdbcUrl"));
        config.setJdbcClass(getOption("jdbcDriver"));
        config.setJdbcUser(getOption("dbUser"));
        config.setJdbcPass( jdbcPass);
        if(jdbcPwdfile != null && jdbcPwdfile.trim().length() > 0 ) {
            config.setJdbcPwdfile(jdbcPwdfile);
            _log.info("jdbc info : JDBC Password file:"+jdbcPwdfile);
        }

        // scheduler parameters

        config.setGetReqTQueueSize( getIntOption("get-req-thread-queue-size",
            config.getGetReqTQueueSize()));
        config.setGetThreadPoolSize(getIntOption("get-req-thread-pool-size",
            config.getGetThreadPoolSize()));
        config.setGetMaxWaitingRequests(getIntOption("get-req-max-waiting-requests",
            config.getGetMaxWaitingRequests()));
        config.setGetReadyQueueSize(getIntOption("get-req-ready-queue-size",
            config.getGetReadyQueueSize()));
        config.setGetMaxReadyJobs(getIntOption("get-req-max-ready-requests",
            config.getGetMaxReadyJobs()));
        config.setGetMaxNumOfRetries(getIntOption("get-req-max-number-of-retries",
            config.getGetMaxNumOfRetries()));
        config.setGetRetryTimeout(getLongOption("get-req-retry-timeout",
            config.getGetRetryTimeout()));
        config.setGetMaxRunningBySameOwner(
            getIntOption("get-req-max-num-of-running-by-same-owner",
            config.getGetMaxRunningBySameOwner()));

        config.setBringOnlineReqTQueueSize( getIntOption("bring-online-req-thread-queue-size",
            config.getBringOnlineReqTQueueSize()));
        config.setBringOnlineThreadPoolSize(getIntOption("bring-online-req-thread-pool-size",
            config.getBringOnlineThreadPoolSize()));
        config.setBringOnlineMaxWaitingRequests(getIntOption("bring-online-req-max-waiting-requests",
            config.getBringOnlineMaxWaitingRequests()));
        config.setBringOnlineReadyQueueSize(getIntOption("bring-online-req-ready-queue-size",
            config.getBringOnlineReadyQueueSize()));
        config.setBringOnlineMaxReadyJobs(getIntOption("bring-online-req-max-ready-requests",
            config.getBringOnlineMaxReadyJobs()));
        config.setBringOnlineMaxNumOfRetries(getIntOption("bring-online-req-max-number-of-retries",
            config.getBringOnlineMaxNumOfRetries()));
        config.setBringOnlineRetryTimeout(getLongOption("bring-online-req-retry-timeout",
            config.getBringOnlineRetryTimeout()));
        config.setBringOnlineMaxRunningBySameOwner(
            getIntOption("bring-online-req-max-num-of-running-by-same-owner",
            config.getBringOnlineMaxRunningBySameOwner()));


        config.setLsReqTQueueSize( getIntOption("ls-request-thread-queue-size",
            config.getLsReqTQueueSize()));
        config.setLsThreadPoolSize(getIntOption("ls-request-thread-pool-size",
            config.getLsThreadPoolSize()));
        config.setLsMaxWaitingRequests(getIntOption("ls-request-max-waiting-requests",
            config.getLsMaxWaitingRequests()));
        config.setLsReadyQueueSize(getIntOption("ls-request-ready-queue-size",
            config.getLsReadyQueueSize()));
        config.setLsMaxReadyJobs(getIntOption("ls-request-max-ready-requests",
            config.getLsMaxReadyJobs()));
        config.setLsMaxNumOfRetries(getIntOption("ls-request-max-number-of-retries",
            config.getLsMaxNumOfRetries()));
        config.setLsRetryTimeout(getLongOption("ls-request-retry-timeout",
            config.getLsRetryTimeout()));
        config.setLsMaxRunningBySameOwner(
            getIntOption("ls-request-max-num-of-running-by-same-owner",
            config.getLsMaxRunningBySameOwner()));


        config.setPutReqTQueueSize(getIntOption("put-req-thread-queue-size",
            config.getPutReqTQueueSize()));
        config.setPutThreadPoolSize(getIntOption("put-req-thread-pool-size",
            config.getPutThreadPoolSize()));
        config.setPutMaxWaitingRequests(getIntOption("put-req-max-waiting-requests",
            config.getPutMaxWaitingRequests()));
        config.setPutReadyQueueSize(getIntOption("put-req-ready-queue-size",
            config.getPutReadyQueueSize()));
        config.setPutMaxReadyJobs(getIntOption("put-req-max-ready-requests",
            config.getPutMaxReadyJobs()));
        config.setPutMaxNumOfRetries(getIntOption("put-req-max-number-of-retries",
            config.getPutMaxNumOfRetries()));
        config.setPutRetryTimeout(getLongOption("put-req-retry-timeout",
            config.getPutRetryTimeout()));
        config.setPutMaxRunningBySameOwner(
             getIntOption("put-req-max-num-of-running-by-same-owner",
            config.getPutMaxRunningBySameOwner()));
        config.setCopyReqTQueueSize(getIntOption("copy-req-thread-queue-size",
            config.getCopyReqTQueueSize()));
        config.setCopyThreadPoolSize(getIntOption("copy-req-thread-pool-size",
            config.getCopyThreadPoolSize()));
        config.setCopyMaxWaitingRequests(getIntOption("copy-req-max-waiting-requests",
            config.getCopyMaxWaitingRequests()));
        config.setCopyMaxNumOfRetries(getIntOption("copy-req-max-number-of-retries",
            config.getCopyMaxNumOfRetries()));
        config.setCopyRetryTimeout(getLongOption("copy-req-retry-timeout",
            config.getCopyRetryTimeout()));
        config.setCopyMaxRunningBySameOwner(
            getIntOption("copy-req-max-num-of-running-by-same-owner",
            config.getCopyMaxRunningBySameOwner()));

        config.setConnect_to_wsdl(isOptionSetToTrueOrYes("connect-to-wsdl",
            config.isConnect_to_wsdl()));
        config.setStorage_info_update_period( getLongOption(
            "storage-info-update-period",
            config.getStorage_info_update_period()));

        config.setVacuum(isOptionSetToTrueOrYes( "vacuum",
            config.isVacuum()));
        config.setVacuum_period_sec( getLongOption("vacuum-period",
            config.getVacuum_period_sec()));

        config.setGetRequestRestorePolicy(getOption("get-request-restore-policy",
            config.getGetRequestRestorePolicy()));
        config.setBringOnlineRequestRestorePolicy(getOption("bring-online-request-restore-policy",
            config.getBringOnlineRequestRestorePolicy()));
        config.setLsRequestRestorePolicy(getOption("ls-request-restore-policy",
            config.getLsRequestRestorePolicy()));
        config.setPutRequestRestorePolicy(getOption("put-request-restore-policy",
            config.getPutRequestRestorePolicy()));
        config.setCopyRequestRestorePolicy(getOption("copy-request-restore-policy",
            config.getCopyRequestRestorePolicy()));

        LOGINBROKERINFO_VALIDITYSPAN = getLongOption("login-broker-update-period",
            LOGINBROKERINFO_VALIDITYSPAN);

        numDoorInRanSelection = getIntOption("num-doors-in-rand-selection",
            numDoorInRanSelection);

        config.setNumDaysHistory(getIntOption("num-days-history",
            config.getNumDaysHistory()));
        config.setOldRequestRemovePeriodSecs(
            getLongOption("old-request-remove-period-secs",
            config.getOldRequestRemovePeriodSecs()));

        if( _args.getOpt("max-queued-jdbc-tasks-num") != null) {
            config.setMaxQueuedJdbcTasksNum(Integer.valueOf(getIntOption(
                "max-queued-jdbc-tasks-num")));
        }

        if( _args.getOpt("jdbc-execution-thread-num") != null) {
            config.setJdbcExecutionThreadNum(Integer.valueOf(getIntOption(
                "jdbc-execution-thread-num")));
        }

        config.setCredentialsDirectory(getOption("credentials-dir",
            config.getCredentialsDirectory()));

        config.setJdbcLogRequestHistoryInDBEnabled(isOptionSetToTrueOrYes("log-request-history-in-db-enabled",
            config.isJdbcLogRequestHistoryInDBEnabled())); // false by default

        config.setCleanPendingRequestsOnRestart(isOptionSetToTrueOrYes("clean-pending-requests-on-restart",
            config.isCleanPendingRequestsOnRestart())); // false by default

        config.setOverwrite(isOptionSetToTrueOrYes("overwrite",
            config.isOverwrite())); //false by default

        config.setOverwrite_by_default(isOptionSetToTrueOrYes("overwrite_by_default",
            config.isOverwrite_by_default())); //false by default

        customGetHostByAddr = isOptionSetToTrueOrYes("custom-get-host-by-addr",
            customGetHostByAddr);

        ignoreClientProtocolOrder = isOptionSetToTrueOrYes(
            "ignore-client-protocol-order",ignoreClientProtocolOrder);


        config.setCredentialsDirectory(getOption("credentials-dir",
            config.getCredentialsDirectory()));

		config.setQosPluginClass(getOption("qosPluginClass",config.getQosPluginClass()));
		config.setQosConfigFile(getOption("qosConfigFile",config.getQosConfigFile()));

        config.setClientDNSLookup(isOptionSetToTrueOrYes("client-dns-lookup",
        config.isClientDNSLookup())); // false by default

        config.setCounterRrdDirectory(getOption("counterRrdDirectory",config.getCounterRrdDirectory()));
        config.setGaugeRrdDirectory(getOption("gaugeRrdDirectory",config.getGaugeRrdDirectory()));

        config.setSrmCounterRrdDirectory(getOption("srmCounterRrdDirectory",config.getSrmCounterRrdDirectory()));
        config.setSrmGaugeRrdDirectory(getOption("srmGaugeRrdDirectory",config.getSrmGaugeRrdDirectory()));


        _log.info("scheduler parameter read, starting");
        this.useInterpreter(true);
        this.getNucleus().export();

        _loginBrokerHandler = new LoginBrokerHandler() ;
        addCommandListener( _loginBrokerHandler ) ;

        this.start();
        try {
            Thread.sleep(5000);
        } catch(InterruptedException ie) {
        }
        AuthRecordPersistenceManager authRecordPersistenceManager =
            new AuthRecordPersistenceManager(
                config.getJdbcUrl(),
                config.getJdbcClass(),
                config.getJdbcUser(),
                config.getJdbcPass());
        config.setSrmUserPersistenceManager(authRecordPersistenceManager);

        tmpstr = _args.getOpt("gsissl");
        if(tmpstr !=null) {
            config.setGsissl(tmpstr.equalsIgnoreCase("true"));
            if(config.isGsissl()) {
                config.setWebservice_protocol("https");

                config.setAuthorization(
                    DCacheAuthorization.getDCacheAuthorization(
                    config.getUseGplazmaAuthzCellFlag(),
                    config.getDelegateToGplazmaFlag(),
                    config.getUseGplazmaAuthzModuleFlag(),
                    config.getGplazmaPolicy(),
                    config.getAuthzCacheLifetime(),
                    config.getKpwdfile(),
                    this,
                    authRecordPersistenceManager));
            } else {
                config.setWebservice_protocol("http");
            }

        } else {
            config.setWebservice_protocol("http");
        }

        config.setStorage(this);

        //getNucleus().newThread( new Runnable(){
        //   public void run() {
        try {
            _log.debug("In constructor of Storage, about " +
                       "to instantiate SRM...");

            srm = new SRM(config,name);
            _log.debug("In anonymous inner class, srm instantiated.");
        } catch (Throwable t) {
            _log.warn("Aborted anonymous inner class, error starting srm", t);
            start();
            kill();
        }
        //   }
        //}
        //).start();
        _log.debug("starting storage info update  thread ...");

        storageInfoUpdateThread = getNucleus().newThread(this);
        storageInfoUpdateThread.start();

        _log.debug("In Storage constructor, about to get/set srmInstance.");

        synchronized(syncObj) {
            srmInstance = srm;
            _log.debug("srmInstance is not null, srmInstance="+srmInstance);
            syncObj.notifyAll();
        }

        _log.debug("srmInstance was set, about to exit Storage constructor.");
    }

    private String getOption(String value) {
        String tmpstr = _args.getOpt(value);
        if(tmpstr == null || tmpstr.length() == 0)  {
            throw new IllegalArgumentException("option "+value+" is not set");
        }
        return tmpstr;

    }

    private String getOption(String value, String default_value) {
        String tmpstr = _args.getOpt(value);
        if(tmpstr == null || tmpstr.length() == 0)  {
            return default_value;
        }
       return tmpstr;
    }

    private boolean isOptionSetToTrueOrYes(String value) {
        String tmpstr = _args.getOpt(value);
        return tmpstr != null &&
            (tmpstr.equalsIgnoreCase("true") ||
             tmpstr.equalsIgnoreCase("on")   ||
             tmpstr.equalsIgnoreCase("yes")  ||
             tmpstr.equalsIgnoreCase("enabled") ) ;
    }

    private boolean isOptionSetToTrueOrYes(String value, boolean default_value) {
        String tmpstr = _args.getOpt(value);
       if( tmpstr != null && tmpstr.length() > 0) {
            return
             tmpstr.equalsIgnoreCase("true") ||
             tmpstr.equalsIgnoreCase("on")   ||
             tmpstr.equalsIgnoreCase("yes")  ||
             tmpstr.equalsIgnoreCase("enabled") ;
       } else {
            return default_value;
       }
    }

    private long getLongOption(String value) throws IllegalArgumentException {
        String tmpstr = _args.getOpt(value);
        if(tmpstr == null || tmpstr.length() == 0)  {
            throw new IllegalArgumentException("option "+value+" is not set");
        }
        return Long.parseLong(tmpstr);
    }

    private long getLongOption(String value, long default_value) {
        String tmpstr = _args.getOpt(value);
        if(tmpstr == null || tmpstr.length() == 0)  {
            return default_value;
        }
        return Long.parseLong(tmpstr);
    }

    private int getIntOption(String value) throws IllegalArgumentException {
        String tmpstr = _args.getOpt(value);
        if(tmpstr == null || tmpstr.length() == 0)  {
            throw new IllegalArgumentException("option "+value+" is not set");
        }
        return Integer.parseInt(tmpstr);
    }

    private int getIntOption(String value, int default_value) {
        String tmpstr = _args.getOpt(value);
        if(tmpstr == null || tmpstr.length() == 0)  {
            return default_value;
        }
       return Integer.parseInt(tmpstr);
    }

    public void getInfo( java.io.PrintWriter printWriter ) {
        StringBuffer sb = new StringBuffer();
        sb.append("SRM Cell");
        sb.append(" storage info : ");
        StorageElementInfo info = getStorageElementInfo();
        if(info != null) {
            sb.append(info.toString());
        } else {
            sb.append("  info is not yet available !!!");
        }
        sb.append('\n');
        sb.append(config.toString()).append('\n');
        try {
            srm.printGetSchedulerInfo(sb);
            srm.printPutSchedulerInfo(sb);
            srm.printCopySchedulerInfo(sb);
        } catch (java.sql.SQLException sqle) {
            sqle.printStackTrace(printWriter);
        }
        printWriter.println( sb.toString()) ;
        if( _loginBrokerHandler != null ){
            printWriter.println( "  LoginBroker Info :" ) ;
            _loginBrokerHandler.getInfo( printWriter ) ;
        }
    }

    public CellVersion getCellVersion(){
        return new CellVersion(
        diskCacheV111.util.Version.getVersion(),"$Revision$" );
    }

    public String fh_set_async_ls= " Syntax : set async ls [on|off]  # turn on/off asynchronous srmls execution ";
    public String hh_set_async_ls= " [on|off]  # turn on/off asynchronous srmls execution ";

    public String ac_set_async_ls_$_0_1(Args args) {
        boolean yes = false;
        boolean no  = false;
        for (String s : new String[] { "on", "true", "t", "yes"}) {
            if (s.equalsIgnoreCase(args.argv(0))) {
                yes = true;
                break;
            }
        }
        if (yes == false) {
            for (String s : new String[] { "off", "false", "f", "no"}) {
                if (s.equalsIgnoreCase(args.argv(0))) {
                    no = true;
                    break;
                }
            }
        }
        if (no==false && yes==false) {
            if (args.argc()==0) {
                yes = true;
            }
            else {
                return "Syntax error : "+args.argv(0)+" is unsupported value";
            }
        }
        config.setAsynchronousLs(yes);
        return "asynchronous ls is "+(config.isAsynchronousLs()?"enabled":"disabled");
    }
    public String fh_db_history_log= " Syntax: db history log [on|off] "+
        "# show status or enable db history log ";
    public String hh_db_history_log= " [on|off] " +
        "# show status or enable db history log ";
    public String ac_db_history_log_$_0_1(Args args) {
        if (args.argc()==0) {
            return "db history logging is " +(
                config.isJdbcLogRequestHistoryInDBEnabled()?
                    " enabled":
                    " disabled");
        }
        String on_off= args.argv(0);
        if(!on_off.equals("on") &&
            !on_off.equals("off")) {
            return "syntax error";
        }

        config.setJdbcLogRequestHistoryInDBEnabled(on_off.equals("on"));
        return "db history logging is " +(
                config.isJdbcLogRequestHistoryInDBEnabled()?
                    " enabled":
                    " disabled");
    }

    public String fh_db_debug_history_log= " Syntax: db debug history log [on|off] "+
        "# show status or enable db history log ";
    public String hh_db_debug_history_log= " [on|off] " +
        "# show status or enable db history log ";
    public String ac_db_debug_history_log_$_0_1(Args args) {
        if(args.argc() == 0) {
            return "db debug history logging is " +(
                config.isJdbcLogRequestHistoryInDBEnabled()?
                    " enabled":
                    " disabled");
        }
        String on_off= args.argv(0);
        if(!on_off.equals("on") &&
            !on_off.equals("off")) {
            return "syntax error";
        }

        config.setJdbcLogRequestHistoryInDBEnabled(on_off.equals("on"));
        return "db debug history logging is " +(
                config.isJdbcLogRequestHistoryInDBEnabled()?
                    " enabled":
                    " disabled");
    }

    public String fh_cancel= " Syntax: cancel <id> ";
    public String hh_cancel= " <id> ";
    public String ac_cancel_$_1(Args args) {
        try {
            Long id = Long.valueOf(args.argv(0));
            StringBuffer sb = new StringBuffer();
            srm.cancelRequest(sb, id);
            return sb.toString();
        } catch (NumberFormatException e) {
            return e.toString();
        } catch (SQLException e) {
            _log.warn(e);
            return e.toString();
        }
    }

    public String fh_cancelall= " Syntax: cancel [-get] [-put] [-copy] [-bring] [-reserve] <pattern> ";
    public String hh_cancelall= " [-get] [-put] [-copy] [-bring] [-reserve] <pattern> ";
    public String ac_cancelall_$_1(Args args) {
        try {
            boolean get=args.getOpt("get") != null;
            boolean put=args.getOpt("put") != null;
            boolean copy=args.getOpt("copy") != null;
            boolean bring=args.getOpt("bring") != null;
            boolean reserve=args.getOpt("reserve") != null;
            boolean ls=args.getOpt("ls") != null;
            if( !get && !put && !copy && !bring && !reserve && !ls ) {
                get=true;
                put=true;
                copy=true;
                bring=true;
                reserve=true;
                ls=true;
            }
            String pattern = args.argv(0);
            StringBuffer sb = new StringBuffer();
            if(get) {
                _log.debug("calling srm.cancelAllGetRequest(\""+pattern+"\")");
                srm.cancelAllGetRequest(sb, pattern);
            }
            if(bring) {
                _log.debug("calling srm.cancelAllBringOnlineRequest(\""+pattern+"\")");
                srm.cancelAllBringOnlineRequest(sb, pattern);
            }
            if(put) {
                _log.debug("calling srm.cancelAllPutRequest(\""+pattern+"\")");
                srm.cancelAllPutRequest(sb, pattern);
            }
            if(copy) {
                _log.debug("calling srm.cancelAllCopyRequest(\""+pattern+"\")");
                srm.cancelAllCopyRequest(sb, pattern);
            }
            if(reserve) {
                _log.debug("calling srm.cancelAllReserveSpaceRequest(\""+pattern+"\")");
                srm.cancelAllReserveSpaceRequest(sb, pattern);
            }
            if(ls) {
                _log.debug("calling srm.cancelAllLsRequests(\""+pattern+"\")");
                srm.cancelAllLsRequests(sb, pattern);
            }
            return sb.toString();
        } catch (RuntimeException e) {
            _log.fatal("Failure in cancelall", e);
            return e.toString();
        } catch (Exception e) {
            _log.warn("Failure in cancelall: " + e.getMessage());
            return e.toString();
        }
    }
    public String fh_ls= " Syntax: ls [-get] [-put] [-copy] [-bring] [-reserve] [-ls] [-l] [<id>] "+
            "#will list all requests";
    public String hh_ls= " [-get] [-put] [-copy] [-bring] [-reserve] [-ls] [-l] [<id>]";
    public String ac_ls_$_0_1(Args args) {
        try {
            boolean get=args.getOpt("get") != null;
            boolean put=args.getOpt("put") != null;
            boolean copy=args.getOpt("copy") != null;
            boolean bring=args.getOpt("bring") != null;
            boolean reserve=args.getOpt("reserve") != null;
            boolean ls=args.getOpt("ls") != null;
            boolean longformat = args.getOpt("l") != null;
            StringBuffer sb = new StringBuffer();
            if(args.argc() == 1) {
                try {
                    Long reqId = Long.valueOf(args.argv(0));
                    srm.listRequest(sb, reqId, longformat);
                } catch( NumberFormatException nfe) {
                    return "id must be an integer, you gave id="+args.argv(0);
                }
            } else {
                if( !get && !put && !copy && !bring && !reserve && !ls) {
                    get=true;
                    put=true;
                    copy=true;
                    bring=true;
                    reserve=true;
                    ls=true;
                }
                if(get) {
                    sb.append("Get Requests:\n");
                    srm.listGetRequests(sb);
                    sb.append('\n');
                }
                if(put) {
                    sb.append("Put Requests:\n");
                    srm.listPutRequests(sb);
                    sb.append('\n');
                }
                if(copy) {
                    sb.append("Copy Requests:\n");
                    srm.listCopyRequests(sb);
                    sb.append('\n');
                }
                if(copy) {
                    sb.append("Bring Online Requests:\n");
                    srm.listBringOnlineRequests(sb);
                    sb.append('\n');
                }
                if(reserve) {
                    sb.append("Reserve Space Requests:\n");
                    srm.listReserveSpaceRequests(sb);
                    sb.append('\n');
                }
                if(ls) {
                    sb.append("Ls Requests:\n");
                    srm.listLsRequests(sb);
                    sb.append('\n');
                }
            }
            return sb.toString();
        } catch(Throwable t) {
            t.printStackTrace();
            return t.toString();
        }
    }
    public String fh_ls_queues= " Syntax: ls queues " +
        "[-get] [-put] [-copy] [-bring] [-ls] [-l]  "+
            "#will list schedule queues";
    public String hh_ls_queues= " [-get] [-put] [-copy] [-bring] [-ls] [-l] ";
    public String ac_ls_queues_$_0(Args args) {
        try {
            boolean get=args.getOpt("get") != null;
            boolean put=args.getOpt("put") != null;
            boolean ls=args.getOpt("ls") != null;
            boolean copy=args.getOpt("copy") != null;
            boolean bring=args.getOpt("bring") != null;
            boolean longformat = args.getOpt("l") != null;
            StringBuffer sb = new StringBuffer();

            if( !get && !put && !copy && !bring && !ls ) {
                get=true;
                put=true;
                copy=true;
                bring=true;
                ls=true;
            }
            if(get) {
                sb.append("Get Request Scheduler:\n");
                srm.printGetSchedulerThreadQueue(sb);
                srm.printGetSchedulerPriorityThreadQueue(sb);
                srm.printCopySchedulerReadyThreadQueue(sb);
                sb.append('\n');
            }
            if(put) {
                sb.append("Put Request Scheduler:\n");
                srm.printPutSchedulerThreadQueue(sb);
                srm.printPutSchedulerPriorityThreadQueue(sb);
                srm.printPutSchedulerReadyThreadQueue(sb);
                sb.append('\n');
            }
            if(copy) {
                sb.append("Copy Request Scheduler:\n");
                srm.printCopySchedulerThreadQueue(sb);
                srm.printCopySchedulerPriorityThreadQueue(sb);
                srm.printCopySchedulerReadyThreadQueue(sb);
                sb.append('\n');
            }
            if(bring) {
                sb.append("Bring Online Request Scheduler:\n");
                srm.printBringOnlineSchedulerThreadQueue(sb);
                srm.printBringOnlineSchedulerPriorityThreadQueue(sb);
                srm.printBringOnlineSchedulerReadyThreadQueue(sb);
                sb.append('\n');
            }
            if(ls) {
                sb.append("Ls Request Scheduler:\n");
                srm.printLsSchedulerThreadQueue(sb);
                srm.printLsSchedulerPriorityThreadQueue(sb);
                srm.printLsSchedulerReadyThreadQueue(sb);
                sb.append('\n');
            }
            return sb.toString();
        } catch(Throwable t) {
            t.printStackTrace();
            return t.toString();
        }
    }

    public String fh_ls_completed= " Syntax: ls completed [-get] [-put]" +
        " [-copy] [-l] [max_count]"+
            " #will list completed (done, failed or canceled) requests, " +
        "if max_count is not specified, it is set to 50";
    public String hh_ls_completed= " [-get] [-put] [-copy] [-l] [max_count]";
    public String ac_ls_completed_$_0_1(Args args) throws Exception{
        boolean get=args.getOpt("get") != null;
        boolean put=args.getOpt("put") != null;
        boolean copy=args.getOpt("copy") != null;
        boolean longformat = args.getOpt("l") != null;
        int max_count=50;
        if(args.argc() == 1) {
            max_count = Integer.parseInt(args.argv(0));
        }

        if( !get && !put && !copy ) {
            get=true;
            put=true;
            copy=true;

        }
        StringBuffer sb = new StringBuffer();
        if(get) {
            sb.append("Get Requests:\n");
            srm.listLatestCompletedGetRequests(sb, max_count);
            sb.append('\n');
        }
        if(put) {
            sb.append("Put Requests:\n");
            srm.listLatestCompletedPutRequests(sb, max_count);
            sb.append('\n');
        }
        if(copy) {
            sb.append("Copy Requests:\n");
            srm.listLatestCompletedCopyRequests(sb, max_count);
            sb.append('\n');
        }
        return sb.toString();
    }

    public String fh_set_job_priority= " Syntax: set priority <requestId> <priority>"+
            "will set priority for the requestid";
    public String hh_set_job_priority=" <requestId> <priority>";

    public String ac_set_job_priority_$_2(Args args) {
        String s1 = args.argv(0);
        String s2 = args.argv(1);
        long requestId;
        int priority;
        try {
            requestId = Integer.parseInt(s1);
        } catch (NumberFormatException e) {
            return "Failed to parse request id: " + s1;
        }
        try {
            priority = Integer.parseInt(s2);
        } catch (Exception e) {
            return "Failed to parse priority: "+s2;
        }
        try {
            Job job = Job.getJob(requestId);
            if(job == null ) {
                return "request with reqiest id "+requestId+" is not found\n";
            }
            job.setPriority(priority);
            job.setPriority(priority);
            StringBuffer sb = new StringBuffer();
            srm.listRequest(sb, requestId, true);
            return sb.toString();
        } catch (RuntimeException e) {
            _log.fatal("Failure in set job priority", e);
            return e.toString();
        } catch (Exception e) {
            _log.warn("Failure in set job priority: " + e.getMessage());
            return e.toString();
        }
    }


    public String fh_set_max_ready_put= " Syntax: set max ready put <count>"+
            " #will set a maximum number of put requests in the ready state";
    public String hh_set_max_ready_put= " <count>";
    public String ac_set_max_ready_put_$_1(Args args) throws Exception{
        if(args.argc() != 1) {
            throw new IllegalArgumentException("count is not specified");
        }
        int value = Integer.parseInt(args.argv(0));
        config.setPutMaxReadyJobs(value);
        srm.getPutRequestScheduler().setMaxReadyJobs(value);
        _log.info("put-req-max-ready-requests="+value);
        return "put-req-max-ready-requests="+value;
    }

    public String fh_set_max_ready_get= " Syntax: set max ready get <count>"+
            " #will set a maximum number of get requests in the ready state";
    public String hh_set_max_ready_get= " <count>";
    public String ac_set_max_ready_get_$_1(Args args) throws Exception{
        if(args.argc() != 1) {
            throw new IllegalArgumentException("count is not specified");
        }
        int value = Integer.parseInt(args.argv(0));
        config.setGetMaxReadyJobs(value);
        srm.getGetRequestScheduler().setMaxReadyJobs(value);
        _log.info("get-req-max-ready-requests="+value);
        return "get-req-max-ready-requests="+value;
    }

    public String fh_set_max_ready_bring_online= " Syntax: set max ready bring online <count>"+
            " #will set a maximum number of bring online requests in the ready state";
    public String hh_set_max_ready_bring_online= " <count>";
    public String ac_set_max_ready_bring_online_$_1(Args args) throws Exception{
        if(args.argc() != 1) {
            throw new IllegalArgumentException("count is not specified");
        }
        int value = Integer.parseInt(args.argv(0));
        config.setBringOnlineMaxReadyJobs(value);
        srm.getBringOnlineRequestScheduler().setMaxReadyJobs(value);
        _log.info("bring-online-req-max-ready-requests="+value);
        return "bring-online-req-max-ready-requests="+value;
    }

    public String fh_set_max_read_ls_= " Syntax: set max read ls <count>\n"+
            " #will set a maximum number of ls requests in the ready state\n"+
            " #\"set max read ls\" is an alias for \"set max ready ls\" preserved for compatibility ";
    public String hh_set_max_read_ls= " <count>";
    public String ac_set_read_ls_$_1(Args args) throws Exception{
        return ac_set_max_ready_ls_$_1(args);
    }

    public String fh_set_max_ready_ls= " Syntax: set max ready ls <count>\n"+
            " #will set a maximum number of ls requests in the ready state";
    public String hh_set_max_ready_ls= " <count>";
    public String ac_set_max_ready_ls_$_1(Args args) throws Exception{
        if(args.argc() != 1) {
            throw new IllegalArgumentException("count is not specified");
        }
        int value = Integer.parseInt(args.argv(0));
        config.setLsMaxReadyJobs(value);
        srm.getLsRequestScheduler().setMaxReadyJobs(value);
        _log.info("ls-request-max-ready-requests="+value);
        return "ls-request-max-ready-requests="+value;
    }

      public String fh_dir_creators_ls= " Syntax: dir creators ls [-l]  "+
         "#will list all put companion waiting for the dir creation ";
      public String hh_dir_creators_ls= " [-l] ";
      public String ac_dir_creators_ls_$_0(Args args) {
        try {
            boolean longformat = args.getOpt("l") != null;
            StringBuffer sb = new StringBuffer();
            PutCompanion.listDirectoriesWaitingForCreation(sb,longformat);
            return sb.toString();
         } catch(Throwable t) {
            t.printStackTrace();
            return t.toString();
         }
      }
      public String fh_cancel_dir_creation= " Syntax:cancel dir creation <path>  "+
         "#will fail companion waiting for the dir creation on <path> ";
      public String hh_cancel_dir_creation= " <path>";
      public String ac_cancel_dir_creation_$_1(Args args) {
        try {
            String pnfsPath = args.argv(0);
            StringBuffer sb = new StringBuffer();
            PutCompanion.failCreatorsForPath(pnfsPath,sb);
            return sb.toString();
         } catch(Throwable t) {
            t.printStackTrace();
            return t.toString();
         }
      }

      public String hh_print_srm_counters= "# prints the counters for all srm operations";
      public String ac_print_srm_counters_$_0(Args args) {
            return srm.getSrmServerV1Counters().toString()+
                    '\n'+
                   srm.getSrmServerV2Counters().toString()+
                   '\n'+
                   srm.getAbstractStorageElementCounters().toString()+
                   '\n'+
                   srm.getSrmServerV1Gauges().toString()+
                   '\n'+
                   srm.getSrmServerV2Gauges().toString()+
                   '\n'+
                   srm.getAbstractStorageElementGauges().toString();
      }


    /**
     * Starts new threads for processing TransferManagerMessage.
     */
    public void messageArrived(final TransferManagerMessage msg)
    {
        diskCacheV111.util.ThreadManager.execute(new Runnable() {
                public void run() {
                    handleTransferManagerMessage(msg);
                }
            });
    }

    private boolean isCached(StorageInfo storage_info, PnfsId _pnfsId) {
         PoolMgrQueryPoolsMsg query =
                 new PoolMgrQueryPoolsMsg( DirectionType.READ ,
                       storage_info.getStorageClass()+"@"+storage_info.getHsm() ,
                       storage_info.getCacheClass(),
                       "*/*",
                       config.getSrmhost(),
                       null);

         CellMessage checkMessage = new CellMessage( _poolMgrPath , query ) ;
         _log.debug("isCached: Waiting for PoolMgrQueryPoolsMsg reply from PoolManager");
         try {
             checkMessage = sendAndWait(  checkMessage , __poolManagerTimeout*1000 ) ;
             if(checkMessage == null) {
                 _log.warn("isCached(): timeout expired");
                 return false;
             }
             query = (PoolMgrQueryPoolsMsg) checkMessage.getMessageObject() ;
         }
 	catch(Exception ee ) {
            _log.warn("isCached(): error receiving message back from PoolManager : "+ee);
             return false;
         }

         if( query.getReturnCode() != 0 ) {
             _log.debug( "storageInfo Available") ;
         }
         try {
             List assumedLocations = _pnfs.getCacheLocations(_pnfsId) ;
             List<String> [] lists = query.getPools() ;
             HashMap hash = new HashMap() ;

             for( int i = 0 ; i < lists.length ; i++ ) {
                 Iterator nn = lists[i].iterator() ;
                 while( nn.hasNext() ) {
                     hash.put( nn.next() , "" ) ;
                 }
             }

             Iterator nn = assumedLocations.iterator() ;

             while( nn.hasNext() ) {
                 if( hash.get( nn.next() ) != null ) {
                     return true;
                 }
             }
         } catch (RuntimeException e) {
             _log.fatal("isCached exception", e);
         } catch (Exception e) {
             _log.error("isCached exception : " + e);
         }
         return false;
     }

    public void log(String s)
    {
        _log.info(s);
    }

    public void elog(String s)
    {
        _log.error(s);
    }

    public void elog(Throwable t)
    {
        _log.fatal(t ,t);
    }

    public void pinFile(SRMUser user,
        String fileId,
        String clientHost,
        FileMetaData fmd,
        long pinLifetime,
        long requestId,
        PinCallbacks callbacks) {
        DcacheFileMetaData dfmd = (DcacheFileMetaData) fmd;
        PinCompanion.pinFile((AuthorizationRecord)user,
            fileId,
            clientHost,
            callbacks, dfmd, pinLifetime, requestId, this);
    }

    public void unPinFile(SRMUser user,String fileId,
            UnpinCallbacks callbacks,
            String pinId) {
        UnpinCompanion.unpinFile((AuthorizationRecord)user, fileId, pinId, callbacks,this);
    }


    public void unPinFileBySrmRequestId(SRMUser user,String fileId,
            UnpinCallbacks callbacks,
            long srmRequestId) {
        UnpinCompanion.unpinFileBySrmRequestId((AuthorizationRecord)user, fileId, srmRequestId, callbacks,this);
    }

    public void unPinFile(SRMUser user,String fileId,
            UnpinCallbacks callbacks) {
        UnpinCompanion.unpinFile((AuthorizationRecord)user, fileId, callbacks,this);
    }


    public String selectGetProtocol(String[] protocols)
    throws SRMException {
        HashSet available_protocols = listAvailableProtocols();
        available_protocols.retainAll(Arrays.asList(protocols));
        available_protocols.removeAll(Arrays.asList(SRM_GET_NOT_SUPPORTED_PROTOCOLS));
        if(available_protocols.size() == 0) {
            _log.error("can not find sutable get protocol");
            throw new SRMException("can not find sutable get protocol");
        }

         /*
          * this is incorrect, need to select on basis of client's preferences
          * But we need to continue doing this while old srmcp clients
          * are out there in the wild
          */
         if(ignoreClientProtocolOrder) {
              for(int i = 0; i<SRM_PREFERED_PROTOCOLS.length; ++i) {
                if(available_protocols.contains(SRM_PREFERED_PROTOCOLS[i])) {
                   return SRM_PREFERED_PROTOCOLS[i];
                }
             }
         }

         for(int i = 0; i<protocols.length; ++i) {
            if(available_protocols.contains(protocols[i])) {
                return protocols[i];
            }
        }

        // we should never get here
        throw new SRMException("can not find sutable get protocol");
    }

    public String selectPutProtocol(String[] protocols)
    throws SRMException {
        HashSet available_protocols = listAvailableProtocols();
        available_protocols.retainAll(Arrays.asList(protocols));
        available_protocols.removeAll(Arrays.asList(SRM_PUT_NOT_SUPPORTED_PROTOCOLS));
        if(available_protocols.size() == 0) {
            _log.error("can not find sutable put protocol");
            throw new SRMException("can not find sutable put protocol");
        }

         /*
          *this is incorrect, need to select on basis of client's preferences
          * But we need to continue doing this while old srmcp clients
          * are out there in the wild
          */
         if(ignoreClientProtocolOrder) {
             for(int i = 0; i<SRM_PREFERED_PROTOCOLS.length; ++i) {
                if(available_protocols.contains(SRM_PREFERED_PROTOCOLS[i])) {
                   return SRM_PREFERED_PROTOCOLS[i];
                }
             }
         }


        for(int i = 0; i<protocols.length; ++i) {
            if(available_protocols.contains(protocols[i])) {
                return protocols[i];
            }
        }

        // we should never get here
        throw new SRMException("can not find sutable put protocol");
    }

    public String[] supportedGetProtocols()
    throws SRMException {
        HashSet protocols = this.listAvailableProtocols();
        return (String[]) protocols.toArray(new String[0]);
    }

    public String[] supportedPutProtocols()
    throws SRMException {
        HashSet protocols = this.listAvailableProtocols();
        // "http" is for getting only
        if(protocols.contains("http")) {
            protocols.remove("http");
        }
        return (String[]) protocols.toArray(new String[0]);
    }

    public String selectGetHost(String protocol,String fileId)
    throws SRMException {
        return this.selectHost(protocol);
    }

    public String selectPutHost(String protocol)
    throws SRMException {
        return this.selectHost(protocol);
    }


    public String getGetTurl(SRMUser user,String path,String[] protocols)
    throws SRMException {
        path = srm_root+"/"+path;
        String protocol = selectGetProtocol(protocols);
        return getTurl(path,protocol,user);
    }

    public String getGetTurl(SRMUser user,String filePath,
            String previous_turl)
            throws SRMException {
        String actualFilePath = srm_root+"/"+filePath;
        GlobusURL prev_turl;
        try {
            prev_turl= new GlobusURL(previous_turl);
        } catch (java.net.MalformedURLException e) {
            _log.warn(e);
            throw new SRMException("Illegal previous turl: " + e.getMessage(),
                                   e);
        }

        String host = prev_turl.getHost();
        int port = prev_turl.getPort();
        if(port > 0) {
            host = host+":"+port;
        }
        return getTurl(actualFilePath, prev_turl.getProtocol(),host, user);
    }

    public String getPutTurl(SRMUser user,String path,String[] protocols)
    throws SRMException {
        path=srm_root+"/"+path;
        String protocol = this.selectPutProtocol(protocols);
        return getTurl(path,protocol,user);
    }


    public String getPutTurl(SRMUser user, String filePath, String previous_turl)
    throws SRMException {
        String actualFilePath = srm_root+"/"+filePath;
        GlobusURL prev_turl;
        try {
            prev_turl= new GlobusURL(previous_turl);
        } catch (java.net.MalformedURLException e) {
            throw
                new SRMException("illegal previous turl:" + e.getMessage(), e);
        }
        String host = prev_turl.getHost();
        int port = prev_turl.getPort();
        if(port > 0) {
            host = host+":"+port;
        }
        return getTurl(actualFilePath, prev_turl.getProtocol(),host, user);
    }



    private String getTurl(String path,String protocol,SRMUser user)
    throws SRMException {
        if(path == null) {
            throw new IllegalArgumentException("path is null");
        }
        if(protocol == null) {
            throw new IllegalArgumentException("protocol is null");
        }
        String transfer_path = getTurlPath(path,protocol,user);
        if(transfer_path == null) {
            throw new SRMException("cab not get transfer path");
        }
        String host = selectHost(protocol);
        if(host == null) {
            throw new SRMException(" failed to get host for "+protocol);
        }
        String turl = protocol+"://"+host+"/"+transfer_path;
        _log.debug("getTurl() returns turl="+turl);
        return turl;

    }

    private String getTurl(String path,String protocol,String host,SRMUser user)
    throws SRMException {
        if(path == null) {
            throw new IllegalArgumentException("path is null");
        }
        if(protocol == null) {
            throw new IllegalArgumentException("protocol is null");
        }
        if(host == null) {
            throw new IllegalArgumentException("host is null");
        }
        String transfer_path = getTurlPath(path,protocol,user);
        if(transfer_path == null) {
            throw new SRMException("cab not get transfer path");
        }
        String turl = protocol+"://"+host+"/"+transfer_path;
        _log.debug("getTurl() returns turl="+turl);
        return turl;
    }

    private boolean verifyUserPathIsRootSubpath(String absolutePath, SRMUser user) {

        if(absolutePath == null) {
            return false;
        }
        String user_root = null;
        if(user != null) {
            AuthorizationRecord duser = (AuthorizationRecord) user;
            user_root = duser.getRoot();
            if(user_root != null) {
                user_root =new FsPath(user_root).toString();
            }
        }


        absolutePath = new FsPath(absolutePath).toString();
        if(user_root!= null) {
            _log.debug("getTurl() user root is "+user_root);
            if(!absolutePath.startsWith(user_root)) {
                String error = "verifyUserPathIsInTheRoot error:"+
                        "user's path "+absolutePath+
                        " is not subpath of the user's root" +user_root;
                _log.warn(error);
                return false;
            }

        }
        return true;
    }

    private String stripRootPath(FsPath root, FsPath path)
        throws SRMAuthorizationException
    {
        if (!path.startsWith(root)) {
            throw new SRMAuthorizationException(String.format("Access denied for path [%s]", path));
        }

        List<String> l = path.getPathItemsList();
        return FsPath.toString(l.subList(root.getPathItemsList().size(),
                                         l.size()));
    }

    private String getTurlPath(String path, String protocol, SRMUser user)
        throws SRMException
    {
        FsPath fullPath = new FsPath(path);
        FsPath userRoot = new FsPath();
        if (user != null) {
            AuthorizationRecord duser = (AuthorizationRecord) user;
            String root = duser.getRoot();
            if (root != null) {
                userRoot = new FsPath(root);
            }
        }

        if (!verifyUserPathIsRootSubpath(path, user)) {
            throw new SRMAuthorizationException(String.format("Access denied: Path [%s] is outside user's root [%s]", path, userRoot));
        }

        String transferPath;
        if (protocol.equals("gsiftp")) {
            transferPath = stripRootPath(userRoot, fullPath);
        } else if (protocol.equals("http")) {
            transferPath = stripRootPath(_httpRootPath, fullPath);
        } else if (protocol.equals("root")) {
            transferPath = stripRootPath(_xrootdRootPath, fullPath);
        } else {
            transferPath = fullPath.toString();
        }

        _log.debug("getTurlPath(path=" + path + ",protocol=" + protocol +
            ",user=" + user + ") = " + transferPath);

        return transferPath;
    }

    public LoginBrokerInfo[] getLoginBrokerInfos()
    throws SRMException {
        return getLoginBrokerInfos((String)null);
    }

    public static final int MAX_LOGIN_BROKER_RETRIES=5;

    private class LoginBrokerCompanion implements CellMessageAnswerable {
        public boolean answered = false;
        public LoginBrokerInfo[] loginBrokerInfos ; //null by default
        public String error; //null by default, error is indicated by error being nonnull

        public void answerArrived( CellMessage request ,
                CellMessage answer    ) {
            int i = 0;
            if(answer == null) {
                failedNotify("getLoginBrokerInfos: answer == null");
                return;
            }
            Object o = answer.getMessageObject();
            if(o == null ) {
                failedNotify("getLoginBrokerInfos: loginBroker answer is null");
                return;
            }

            if(!(o instanceof  dmg.cells.services.login.LoginBrokerInfo[]) ) {
                failedNotify("getLoginBrokerInfos: login broker returned o ="+o);
                return;
            }
            LoginBrokerInfo[] _loginBrokerInfos = (LoginBrokerInfo[]) o;

            successNotify(_loginBrokerInfos);
        }

        public void exceptionArrived( CellMessage request ,
                Exception   exception ) {
            failedNotify("request failed: "+exception);
        }

        public void answerTimedOut( CellMessage request ) {
            failedNotify("request timed out");
        }

        public synchronized void successNotify(LoginBrokerInfo[] loginBrokerInfos ) {
            this.loginBrokerInfos =loginBrokerInfos ;
            answered = true;
            notifyAll();
        }

        public synchronized void failedNotify(String error) {
            _log.error("LoginBrokerCompanion.failedNotify("+error+")");
            this.error =error ;
            answered = true;
            notifyAll();
        }

        public void waitForAnswer() throws InterruptedException {
            while(true) {
                synchronized(this) {
                    wait(1000);
                    if(answered) {
                        return;
                    }
                }
            }
        }
    }
    //these hashtables will be used as a caching mechanizm for the login broker infos
    // here we asume that no protocol called "null" is going to be ever used
    private java.util.Hashtable latestLoginBrokerInfos = new java.util.Hashtable();
    private java.util.Hashtable latestLoginBrokerInfosTimes =
            new java.util.Hashtable();
    // 30 secs in millisecond
    private long LOGINBROKERINFO_VALIDITYSPAN =30*1000;

    public LoginBrokerInfo[] getLoginBrokerInfos(String protocol)
    throws SRMException {
        String key = protocol == null ? "null" : protocol;
        Object o = latestLoginBrokerInfosTimes.get(key);
        if(o !=null) {
            Long timestamp = (Long) o;
            if(System.currentTimeMillis() - timestamp.longValue() <
                    LOGINBROKERINFO_VALIDITYSPAN) {
                o = latestLoginBrokerInfos.get(key);
                if(o != null) {
                    return (LoginBrokerInfo[]) o;
                }
            }
        }

        if(loginBrokerPath == null) {
            loginBrokerPath = new CellPath(loginBrokerName);
        }

        String brokerMessage = "ls -binary";
        if(protocol != null) {
            brokerMessage = brokerMessage+" -protocol="+protocol;
        }
        LoginBrokerCompanion companion = null;
        for(int retry=0;retry<MAX_LOGIN_BROKER_RETRIES;++retry) {
            companion = new LoginBrokerCompanion();
            _log.debug("getLoginBrokerInfos sending \""+brokerMessage+
                       "\"  to LoginBroker");
            sendMessage(new CellMessage(loginBrokerPath,brokerMessage) ,
                        companion,
                        __pnfsTimeout*1000) ;
            try {
                companion.waitForAnswer();
            } catch (InterruptedException ie) {
                // ignore
            }
            if(companion.error != null) {
                try {
                    Thread.sleep(5*1000);
                } catch (InterruptedException ie) {
                    // ignore
                }
                continue;
            }
            break;
        }

        if(companion.error != null) {
            throw new SRMException(" communication with Login Broker failed with"+
                companion.error);
        }
        latestLoginBrokerInfosTimes.put(key, Long.valueOf(System.currentTimeMillis()));
        latestLoginBrokerInfos.put(key,  companion.loginBrokerInfos);
        return companion.loginBrokerInfos;
    }

    public HashSet listAvailableProtocols()
    throws SRMException {
        HashSet protocols = new HashSet();
        LoginBrokerInfo[] loginBrokerInfos = getLoginBrokerInfos();
        int len = loginBrokerInfos.length;
        for(int i = 0; i<len; ++i) {
            String protocol = loginBrokerInfos[i].getProtocolFamily();
            if(!protocols.contains(protocol)) {
                protocols.add(protocol);
            }

        }

        return protocols;
    }

    public boolean isLocalTransferUrl(String url)
    throws SRMException {

        GlobusURL gurl;
        try {
            gurl = new GlobusURL(url);
        } catch(MalformedURLException mue) {
            return false;
        }
        String protocol = gurl.getProtocol();
        LoginBrokerInfo[] loginBrokerInfos = getLoginBrokerInfos(protocol);
        if(loginBrokerInfos.length == 0) {
            return false;
        }
        String host = gurl.getHost();
        int port = gurl.getPort();

        for(int i=0;i<loginBrokerInfos.length;++i) {
            if(loginBrokerInfos[i].getHost().equals(host)  &&
                    loginBrokerInfos[i].getPort() == port) {
                return true;
            }
        }

        return false;
    }


    public String selectHost(String protocol)
        throws SRMException
    {
        _log.debug("selectHost("+protocol+")");
        boolean tryFile = false;
        LoginBrokerInfo[]loginBrokerInfos = getLoginBrokerInfos(protocol);
        return selectHost(loginBrokerInfos);
    }

    private Random rand = new Random();

    int numDoorInRanSelection=3;

   /**
     *  HostnameCacheRecord TTL in millis
     */
    private static final long doorToHostnameCacheTTL = 600000L; // ten minutes

    private static final class HostnameCacheRecord {
        private String hostname;
        /**
         *  in millis
         */
        private long  creationTime;
        public HostnameCacheRecord( String hostname ) {
            this.hostname = hostname;
            this.creationTime = System.currentTimeMillis();
        }
        /**
         * @return the hostname
         */
        public String getHostname() {
            return hostname;
        }

        public boolean expired() {
            return
                System.currentTimeMillis() - creationTime >
                doorToHostnameCacheTTL;
        }

    }
     private Map<String,HostnameCacheRecord> doorToHostnameMap =
            new HashMap<String,HostnameCacheRecord>();
    private String lbiToDoor(LoginBrokerInfo lbi) throws SRMException {

            String thehost =lbi.getHost();
            String resolvedHost;
            HostnameCacheRecord resolvedHostRecord = doorToHostnameMap.get(thehost);
            if(resolvedHostRecord == null || resolvedHostRecord.expired() ) {
                try {

                    InetAddress address = InetAddress.getByName(thehost);
                    resolvedHost = address.getHostName();
                    if ( customGetHostByAddr && resolvedHost.toUpperCase().equals(
                        resolvedHost.toLowerCase() )  ) {// must be an IP address
                            resolvedHost = getHostByAddr( address.getAddress() );
                    }
                } catch (IOException e) {
                    throw new SRMException("selectHost " + e, e);
                }
                // cache record
                doorToHostnameMap.put(thehost,
                        new HostnameCacheRecord(resolvedHost));
            } else {
                resolvedHost = resolvedHostRecord.getHostname();
            }


            return resolvedHost+":"+ lbi.getPort();

    }
    public String selectHost(LoginBrokerInfo[]loginBrokerInfos)
    throws SRMException {
        java.util.Arrays.sort(loginBrokerInfos,new java.util.Comparator(){
            public int compare(java.lang.Object o1,java.lang.Object o2) {
                LoginBrokerInfo loginBrokerInfo1 = (LoginBrokerInfo)o1;
                LoginBrokerInfo loginBrokerInfo2 = (LoginBrokerInfo)o2;
                double load1 = loginBrokerInfo1.getLoad();
                double load2 = loginBrokerInfo2.getLoad();
                if(load1<load2) { return -1  ;}
                if(load1==load2) { return 0  ;}
                return 1;
            } });
            int len = loginBrokerInfos.length;

            if(len <=0){
                return null;
            }

            int selected_indx = rand.nextInt(java.lang.Math.min(len,
                numDoorInRanSelection));
            String doorHostPort = lbiToDoor(loginBrokerInfos[selected_indx]);

            _log.debug("selectHost returns "+doorHostPort);
            return doorHostPort;
    }


    /**
     * Next two functions are
     * BNL's contribution
     */

        private static Map resolve(String name, String[] attrIds)
            throws NamingException {

            Map map = new HashMap();
            DirContext ctx = new InitialDirContext();
            javax.naming.directory.Attributes attrs =
                    ctx.getAttributes(name, attrIds);

            if (attrs == null) {
                return null;
            } else {
                /* get each attribute */
                for (NamingEnumeration ae = attrs.getAll();
                     ae != null && ae.hasMoreElements();) {
                    javax.naming.directory.Attribute attr =
                            (javax.naming.directory.Attribute)ae.next();
                    String attrID = attr.getID();
                    java.util.List l = new java.util.ArrayList();
                    for (NamingEnumeration e = attr.getAll();
                         e.hasMoreElements();) {
                        String literalip = (String)e.nextElement();
                        l.add(literalip);
                    }
                    map.put(attrID, l);
                }
            }
            return map;
        }

        private static String getHostByAddr(byte[] addr)
        throws java.net.UnknownHostException {
            try {
                StringBuilder literalip = new StringBuilder();
                if (addr.length == 4) {
                    for (int i = addr.length-1; i >= 0; i--) {
                        literalip.append(addr[i] & 0xff).append(".");
                    }
                } else if (addr.length == 16) {
                    for (int i = addr.length-1; i >= 0; i--) {
                        literalip.append(addr[i] & 0x0f).append(".").append(addr[i] & 0xf0).append(".");
                    }
                }
                if (addr.length == 4) { // ipv4 addr
                    literalip.append("IN-ADDR.ARPA.");
                } else if (addr.length == 16) { // ipv6 addr
                    literalip.append("IP6.INT.");
                }

                String[] ids = new String[1];
                ids[0] = "PTR"; // PTR record
                Map map = resolve("dns:///" + literalip, ids);
                Set entrySet = map.entrySet();
                String host = "";
                for (Iterator itr = entrySet.iterator(); itr.hasNext();) {
                    Map.Entry e = (Map.Entry)itr.next();
                    host = (String)((java.util.ArrayList)e.getValue()).get(0);
                }
                return host;
            } catch (Exception e) {
                throw new java.net.UnknownHostException(e.getMessage());
            }
        }

    public void getFileInfo(SRMUser user, String filePath,
        GetFileInfoCallbacks callbacks) {
        String actualPnfsPath= srm_root+"/"+filePath;
        if(!verifyUserPathIsRootSubpath(actualPnfsPath,user)) {
            callbacks.GetStorageInfoFailed("user's path ["+actualPnfsPath+
                    "] is not a subpath of user's root ");
        }
        GetFileInfoCompanion.getFileInfo(
                (AuthorizationRecord)user,
                actualPnfsPath,
                callbacks,
                this);
    }

    public void prepareToPut(SRMUser user,
            String filePath,
            PrepareToPutCallbacks callbacks,
            boolean overwrite) {
        String actualPnfsPath = srm_root+"/"+filePath;
        PutCompanion.PrepareToPutFile(
                (AuthorizationRecord)user,
                actualPnfsPath,
                callbacks,
                this,
                config.isRecursiveDirectoryCreation(),
                overwrite);
    }



    public void setFileMetaData(SRMUser user, FileMetaData fmd)
        throws SRMException {
        DcacheFileMetaData dfmd=null;
        if(!(fmd instanceof DcacheFileMetaData)) {
                throw new SRMException("Storage.setFileMetaData: " +
                        "metadata in not dCacheMetaData");
        }
        dfmd = (DcacheFileMetaData) fmd;
        _log.info("Storage.setFileMetaData("+dfmd.getFmd()+
                  " , size="+dfmd.getFmd().getFileSize()+")");


        dfmd.getFmd().setUserPermissions(
                new diskCacheV111.util.FileMetaData.Permissions (
                (dfmd.permMode >> 6 ) & 0x7 ) ) ;
        dfmd.getFmd().setGroupPermissions(
                new diskCacheV111.util.FileMetaData.Permissions (
                (dfmd.permMode >> 3 ) & 0x7 )) ;
        dfmd.getFmd().setWorldPermissions(
                new diskCacheV111.util.FileMetaData.Permissions(
                dfmd.permMode  & 0x7 ) ) ;


        long time = System.currentTimeMillis()/1000L;
        dfmd.getFmd().setLastAccessedTime(time);
        dfmd.getFmd().setLastModifiedTime(time);

        AuthorizationRecord duser = null;
        if (user != null && user instanceof AuthorizationRecord) {
                duser = (AuthorizationRecord) user;
        }

        PnfsSetFileMetaDataMessage msg =
            new PnfsSetFileMetaDataMessage(dfmd.getPnfsId());
        msg.setMetaData(dfmd.getFmd());
        msg.setReplyRequired(true);
        CellMessage answer=null;
        Object o=null;
        try {
                answer = sendAndWait(new CellMessage(
                                             new CellPath("PnfsManager") ,
                                             msg) ,
                                     __pnfsTimeout*1000);
        } catch (NoRouteToCellException e) {
            throw new SRMException("PnfsManager is unavailable: "+ e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new SRMException("Request was interrupted", e);
        }
        if (answer == null ||
            (o = answer.getMessageObject()) == null ||
            !(o instanceof PnfsSetFileMetaDataMessage)) {
               _log.warn("sent PnfsSetFileMetaDataMessage, received "+o+" back");
                throw new SRMException("can set metadata "+fmd.SURL);
        }
        else {
            PnfsSetFileMetaDataMessage reply  =
                (PnfsSetFileMetaDataMessage) answer.getMessageObject();
            if (reply.getReturnCode() != 0) {
                    throw new SRMException("SetFileMetaData Failed : "+fmd.SURL+
                                           "SetFileMetaData  return code="+
                        reply.getReturnCode()+
                                           " reason : "+reply.getErrorObject());
            }
        }
    }


    public FileMetaData getFileMetaData(SRMUser user,
					String path)
        throws SRMException  {
        return getFileMetaData(user, path, null);
    }

    public FileMetaData getFileMetaData(SRMUser user,
					String path,
                                        FileMetaData parentFMD)
        throws SRMException {
        _log.debug("getFileMetaData(" + path + ")");
        String absolute_path = srm_root + "/" + path;
        diskCacheV111.util.FileMetaData parent_util_fmd = null;
        if (parentFMD != null && parentFMD instanceof DcacheFileMetaData) {
            DcacheFileMetaData dfmd = (DcacheFileMetaData)parentFMD;
            parent_util_fmd = dfmd.getFmd();
        }
        AuthorizationRecord duser = null;
        if (user != null && user instanceof AuthorizationRecord) {
            duser = (AuthorizationRecord) user;
        }
        FsPath parent_path = new FsPath(absolute_path);
        parent_path.add("..");
        String parent = parent_path.toString();
        diskCacheV111.util.FileMetaData util_fmd = null;
        StorageInfo storage_info = null;
        PnfsId pnfsId;
        Set<Checksum> checksums;
        try {
            PnfsGetStorageInfoMessage storage_info_msg = null;
            PnfsGetFileMetaDataMessage filemetadata_msg = null;
            try {
                storage_info_msg = _pnfs.getStorageInfoByPath(absolute_path,
                    true);
            }
	    catch (CacheException e) {
                filemetadata_msg = _pnfs.getFileMetaDataByPath(absolute_path,
                    true,
                    true);
            }
            if (storage_info_msg != null) {
                storage_info = storage_info_msg.getStorageInfo();
                util_fmd = storage_info_msg.getMetaData();
                pnfsId = storage_info_msg.getPnfsId();
                checksums = storage_info_msg.getChecksums();
            }
	    else if(filemetadata_msg != null) {
                util_fmd = filemetadata_msg.getMetaData();
                pnfsId = filemetadata_msg.getPnfsId();
                checksums = filemetadata_msg.getChecksums();
            }
	    else {
                throw new SRMException(
                    "could not get storage info or file metadata by path ");

            }
            if (duser == null) {
                if (parent_util_fmd == null) {
                    PnfsGetFileMetaDataMessage parent_filemetadata_msg =
                        _pnfs.getFileMetaDataByPath(parent);
                    parent_util_fmd = parent_filemetadata_msg.getMetaData();
                }
                if (!permissionHandler.worldCanRead(
                    absolute_path, parent_util_fmd, util_fmd)) {
                    throw new SRMAuthorizationException("getFileMetaData have no read " +
                                                           "permission (or file does not exists) ");
                }
            }
        } catch (CacheException e) {
            throw new SRMException("could not get storage info by path: " +
                                   e.getMessage(), e);
        }

        FileMetaData fmd =
            getFileMetaData(user, absolute_path, pnfsId,
                            storage_info, util_fmd,checksums);
        if (storage_info != null) {
		fmd.isCached = isCached(storage_info, pnfsId);
        }

	try {
	    GetFileSpaceTokensMessage getSpaceTokensMessage =
                new GetFileSpaceTokensMessage(pnfsId);
	    CellMessage answer =  sendAndWait(
                new CellMessage(
                new CellPath("SrmSpaceManager"),getSpaceTokensMessage),
					      60*60*1000);
            if(answer==null) {
                _log.error("Failed to retrieve space reservation tokens for file "+
                    absolute_path+"("+pnfsId+")");
            }
	    else {
                Object messageObject =answer.getMessageObject();
                if(messageObject instanceof GetFileSpaceTokensMessage) {
                    getSpaceTokensMessage =
                        (GetFileSpaceTokensMessage)answer.getMessageObject();
                    if (getSpaceTokensMessage.getReturnCode() != 0) {
                        _log.warn("Failed to retrieve space reservation tokens for file "+
                            absolute_path+"("+pnfsId+")");
                    }
                    else {
                        if (getSpaceTokensMessage.getSpaceTokens()!=null) {
                            fmd.spaceTokens =
                                new long[getSpaceTokensMessage.getSpaceTokens().length];
                            System.arraycopy(getSpaceTokensMessage.getSpaceTokens(),0,
                                 fmd.spaceTokens,0,
                                getSpaceTokensMessage.getSpaceTokens().length);
                        }
                    }
                }
                else {
                    _log.error("Failed to retrieve space reservation tokens for file "+
                    absolute_path+"("+pnfsId+") : "+messageObject);
                }
	    }
        } catch (RuntimeException e) {
	    _log.fatal("getFileMetaData failed", e);
        } catch (Exception e) {
	    _log.warn("getFileMetaData failed: " + e);
	}
        return fmd;
    }

    public static FileMetaData
        getFileMetaData(SRMUser user,
                        String absolute_path,
                        PnfsId pnfsId,
                        StorageInfo storage_info,
                        diskCacheV111.util.FileMetaData util_fmd)
    {
        return getFileMetaData(user,absolute_path, pnfsId,storage_info, util_fmd, null);
    }

    public static FileMetaData
        getFileMetaData(SRMUser user,
                        String absolute_path,
                        PnfsId pnfsId,
                        StorageInfo storage_info,
                        diskCacheV111.util.FileMetaData util_fmd,
                        Set<Checksum> checksums)
    {
        boolean isRegular = false;
        boolean isLink = false;
        boolean isDirectory = false;
        boolean isPinned = false;
        boolean isPermanent = true;
        long creationTime = 0;
        long lastModificationTime = 0;
        long lastAccessTime = 0;
        long size = 0;
        String group = null;
        String owner = null;
        String checksum_type = null;
        String checksum_value = null;
        int permissions = 0;
	boolean isStored=false;
        DcacheFileMetaData fmd = new DcacheFileMetaData(pnfsId);

        if (util_fmd != null) {


            if(checksums != null) {
                //first try to find the adler32 checksum
                for(Checksum checksum:checksums) {
                    if(checksum.getType() ==ChecksumType.ADLER32 ) {
                        checksum_type = "adler32";
                        checksum_value = checksum.getValue();
                    }
                }
                //if this failed, but there are other types
                // use the first one found
                if(checksum_type == null && !checksums.isEmpty() ) {
                     Checksum cksum = checksums.iterator().next();
                     checksum_type = cksum.getType().getName().toLowerCase();
                     checksum_value = cksum.getValue();
                }
            }

            owner=Integer.toString(util_fmd.getUid());
            group=Integer.toString(util_fmd.getGid());
            diskCacheV111.util.FileMetaData.Permissions perms =
                util_fmd.getUserPermissions();
            permissions = (perms.canRead()    ? 4 : 0) |
                (perms.canWrite()   ? 2 : 0) |
                (perms.canExecute() ? 1 : 0) ;
            permissions <<= 3;
            perms = util_fmd.getGroupPermissions();
            permissions |=    (perms.canRead()    ? 4 : 0) |
                (perms.canWrite()   ? 2 : 0) |
                (perms.canExecute() ? 1 : 0) ;
            permissions <<= 3;
            perms = util_fmd.getWorldPermissions();
            permissions |=    (perms.canRead()    ? 4 : 0) |
                (perms.canWrite()   ? 2 : 0) |
                (perms.canExecute() ? 1 : 0) ;
            isRegular = util_fmd.isRegularFile();
            isDirectory = util_fmd.isDirectory();
            isLink = util_fmd.isSymbolicLink();
            creationTime=util_fmd.getCreationTime();
            lastModificationTime=util_fmd.getLastModifiedTime();
            lastAccessTime=util_fmd.getLastAccessedTime();
            size = util_fmd.getFileSize();
            fmd.setFmd(util_fmd);
        }
        if (storage_info != null) {
            size = storage_info.getFileSize();
	    TRetentionPolicy retention = null;
	    TAccessLatency latency = null;
	    if (storage_info.getRetentionPolicy() != null) {
		    if(storage_info.getRetentionPolicy().equals(RetentionPolicy.CUSTODIAL)) {
			    retention = TRetentionPolicy.CUSTODIAL;
		    }
		    else if (storage_info.getRetentionPolicy().equals(RetentionPolicy.REPLICA)) {
			    retention = TRetentionPolicy.REPLICA;
		    }
		    else if (storage_info.getRetentionPolicy().equals(RetentionPolicy.OUTPUT)) {
			    retention = TRetentionPolicy.OUTPUT;
		    }
            }
            if (storage_info.getAccessLatency() != null) {
		    if(storage_info.getAccessLatency().equals(AccessLatency.ONLINE)) {
			    latency = TAccessLatency.ONLINE;
		    }
		    else if (storage_info.getAccessLatency().equals(AccessLatency.NEARLINE)) {
			    latency = TAccessLatency.NEARLINE;
		    }
            }
            // RetentionPolicy is non-nillable element of the
            // TRetentionPolicyInfo, if retetion is null, we shold leave
            // the whole retentionPolicyInfo null
            if(retention != null) {
                fmd.retentionPolicyInfo =
                    new TRetentionPolicyInfo(retention, latency);
            }
            fmd.setStorageInfo(storage_info);
	    isStored=storage_info.isStored();
	    if(storage_info.getMap()!=null) {
		    if (storage_info.getMap().get("writeToken")!=null) {
			    fmd.spaceTokens = new long[1];
			    try {
				    fmd.spaceTokens[0] = Long.parseLong(storage_info.getMap().get("writeToken"));
			    }
			    catch (Exception e) {}
		    }
	    }
        }
        fmd.isPinned = isPinned;
        fmd.isPermanent = isPermanent;
        fmd.permMode = permissions;
        fmd.size = size;
        fmd.SURL = null;
        fmd.group = group;
        fmd.owner = owner;
        fmd.checksumType = checksum_type;
        fmd.checksumValue = checksum_value;
        fmd.isDirectory = isDirectory;
        fmd.isRegular = isRegular;
        fmd.isLink = isLink;
        fmd.creationTime = creationTime;
        fmd.lastAccessTime= lastAccessTime;
        fmd.lastModificationTime = lastModificationTime;
	fmd.isStored=isStored;
        return fmd;
    }


    private HashMap idToUserMap = new HashMap();
    private HashMap idToCredentialMap = new HashMap();

    private String getUserById(long id) {
        _log.debug("getDcacheUserById("+id+")");
        synchronized(idToUserMap) {
            return (String) idToUserMap.get(Long.valueOf(id));

        }
    }

    private GSSCredential getCredentialById(long id) {
        _log.debug("getDcacheUserById("+id+")");
        synchronized(idToUserMap) {
            return (GSSCredential) idToCredentialMap.get(Long.valueOf(id));

        }
    }

    private static AtomicLong nextMessageID = new AtomicLong(20000);

    private static synchronized long getNextMessageID()
    {
        return nextMessageID.getAndIncrement();
    }


    public void localCopy(SRMUser user,String fromFilePath, String toFilePath)
    throws SRMException {
        String actualFromFilePath = srm_root+"/"+fromFilePath;
        String actualToFilePath = srm_root+"/"+toFilePath;
        long id = getNextMessageID();
        _log.debug("localCopy for user "+ user+"from actualFromFilePath to actualToFilePath");
        AuthorizationRecord duser = (AuthorizationRecord)user;
        CopyManagerMessage copyRequest =
                new CopyManagerMessage(
                duser.getUid(),
                duser.getGid(),
                actualFromFilePath,
                actualToFilePath,
                id,
                config.getBuffer_size(),
                config.getTcp_buffer_size());

        CellMessage answer;
        try {
            answer = sendAndWait(new CellMessage(
                    new CellPath("CopyManager"),
                    copyRequest),1000*60*60*24);
        } catch (NoRouteToCellException e) {
            throw new SRMException("CopyManager is unavailable: " +
                                   e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new SRMException("Request to CopyManager was interrupted", e);
        }

        if (answer == null) {
            String emsg = "timeout expired while waiting for answer from CopyManager";
            _log.error(emsg);
            throw new SRMException(emsg);
        }
        Object object = answer.getMessageObject();
        if(object == null ||
                !(object instanceof CopyManagerMessage)) {
            String emsg ="failed to recive the "+
                    "CopyMessage back";
            _log.error(emsg);
            throw new SRMException(emsg);
        }
        CopyManagerMessage copyResponse =
                (CopyManagerMessage) object;
        int rc = copyResponse.getReturnCode();
        if( rc != 0) {
            String emsg =" local copy failed with code ="+ rc +
                    " details: "+copyResponse.getDescription();
            _log.warn(emsg);
            throw new SRMException(emsg);
        }
    }


    public void prepareToPutInReservedSpace(SRMUser user, String path, long size,
        long spaceReservationToken, PrepareToPutInSpaceCallbacks callbacks) {
        throw new java.lang.UnsupportedOperationException("NotImplementedException");
    }



    public HashMap poolInfos = new HashMap();
    public HashMap poolInfosTimestamps = new HashMap();

    public StorageElementInfo getPoolInfo(String pool) throws SRMException {
        synchronized(poolInfosTimestamps) {
            if(poolInfosTimestamps.containsKey(pool)) {
                long poolInfoTimestamp =
                        ((Long)(poolInfosTimestamps.get(pool))).longValue();
                if((System.currentTimeMillis() - poolInfoTimestamp) < 3*1000*60) {
                    return (StorageElementInfo) poolInfos.get(pool);
                }
            }
        }

        CellMessage poolInfoMessage = new CellMessage(
                new CellPath((String)( pool)) ,
                "xgetcellinfo" ) ;
        //say("getPoolInfo("+pool+"): Waiting for Pool reply for \"xgetcellinfo\"");
        try {
            poolInfoMessage = sendAndWait(poolInfoMessage,
                    __poolManagerTimeout*1000);
        } catch (NoRouteToCellException e) {
            throw new SRMException("Pool is unavailable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new SRMException("Request to pool was interrupted", e);
        }

        if(poolInfoMessage == null) {
            String error = "pool timeout expired" ;
            _log.error(error);
            throw new SRMException(error);
        }

        Object o = poolInfoMessage.getMessageObject();
        if(o == null || !(o instanceof  PoolCellInfo)) {
            String error = "pool returned o="+o ;
            _log.error(error);
            throw new SRMException(error);
        }
        PoolCellInfo  poolCellInfo = (PoolCellInfo)o;
        PoolCostInfo.PoolSpaceInfo info = poolCellInfo.getPoolCostInfo().getSpaceInfo() ;
        long total     = info.getTotalSpace() ;
        long freespace = info.getFreeSpace() ;
        long precious  = info.getPreciousSpace() ;
        long removable = info.getRemovableSpace() ;
        StorageElementInfo poolInfo = new StorageElementInfo();
        poolInfo.totalSpace = total;
        poolInfo.availableSpace = freespace+removable;
        poolInfo.usedSpace = precious+removable;
        synchronized(poolInfosTimestamps) {
            poolInfos.put(pool,poolInfo);
            poolInfosTimestamps.put(pool, Long.valueOf(System.currentTimeMillis()));
        }
        return poolInfo;

    }

    public void advisoryDelete(final SRMUser user, final String path,
        AdvisoryDeleteCallbacks callbacks) {
        _log.debug("Storage.advisoryDelete");

        if(callbacks == null) {
            callbacks =
                    new AdvisoryDeleteCallbacks() {
                public void AdvisoryDeleteFailed(String reason) {
                    _log.error(" advisoryDelete("+user+","+path+") GetStorageInfoFailed: "+reason);
                }


                public void AdvisoryDeleteSuccesseded(){
                    _log.debug(" advisoryDelete("+user+","+path+") AdvisoryDeleteSuccesseded");
                }

                public void Exception(Exception e){
                    _log.error(" advisoryDelete("+user+","+path+") Exception :"+e);
                }

                public void Timeout(){
                    _log.error(" advisoryDelete("+user+","+path+") timeout");
                }

                public void Error(String error){
                    _log.error(" advisoryDelete("+user+","+path+") Error:" + error);
                }
            };
        }

        String actualPnfsPath= srm_root+"/"+path;
        AdvisoryDeleteCompanion.advisoryDelete(
                (AuthorizationRecord)user,
                actualPnfsPath,
                callbacks,
                this,
                config.isAdvisoryDelete());

    }

    public void removeFile(final SRMUser user,
                           final String path,
                           RemoveFileCallbacks callbacks)
    {
        _log.debug("Storage.removeFile");

        RemoveFileCompanion.removeFile((AuthorizationRecord)user,
                                       getFullPath(path),
                                       callbacks,
                                       _pnfsStub,
                                       this);
    }

    public void removeDirectory(SRMUser user, Vector tree)
        throws SRMException
    {
        _log.debug("Storage.removeDirectory");
        for (Object path: tree) {
            String actualPath = getFullPath(path.toString());
            try {
                _pnfs.deletePnfsEntry(actualPath);
            } catch (TimeoutCacheException e) {
                _log.error("Failed to delete " + actualPath + " due to timeout");
                throw new SRMException("Internal name space timeout while deleting " + path);
            } catch (FileNotFoundCacheException e) {
                throw new SRMException("File does not exist: " + path);
            } catch (NotInTrashCacheException e) {
                throw new SRMException("File does not exist: " + path);
            } catch (CacheException e) {
                _log.error("Failed to delete " + actualPath + ": "
                           + e.getMessage());
                throw new SRMException("Failed to delete " + path + ": "
                                       + e.getMessage());
            }
        }
    }

    public void createDirectory(SRMUser user, String directory)
        throws SRMException
    {
        _log.debug("Storage.createDirectory");

        try {
            /* We copy the mode (permissions) of the parent directory,
             * so we start by querying it. REVISIT: It's a bit strange
             * that PNFS doesn't provide an option to copy the parent
             * mode.
             */
            String path = getFullPath(directory);
            String parent = FsPath.getParent(path);
            FileAttributes attr =
                _pnfs.getFileAttributes(parent, EnumSet.of(FileAttribute.MODE));

            /* Permission checks are performed by PnfsManager because
             * we specify a Subject for the request.
             */
            AuthorizationRecord duser = (AuthorizationRecord) user;
            PnfsHandler handler =
                new PnfsHandler(_pnfs, Subjects.getSubject(duser));
            handler.createPnfsDirectory(path,
                                        duser.getUid(), duser.getGid(),
                                        attr.getMode());
        } catch (TimeoutCacheException e) {
            throw new SRMInternalErrorException("Internal name space timeout", e);
        } catch (NotDirCacheException e) {
            throw new SRMInvalidPathException("Parent path is not a directory", e);
        } catch (NotInTrashCacheException e) {
            throw new SRMInvalidPathException("Parent path does not exist", e);
        } catch (FileNotFoundCacheException e) {
            throw new SRMInvalidPathException("Parent path does not exist", e);
        } catch (FileExistsCacheException e) {
            throw new SRMDuplicationException("File exists");
        } catch (PermissionDeniedCacheException e) {
            throw new SRMAuthorizationException("Permission denied");
        } catch (CacheException e) {
            _log.error("Failed to create directory " + directory + ": "
                       + e.getMessage());
            throw new SRMException(String.format("Failed to create directory [rc=%d,msg=%s]",
                                                 e.getRc(), e.getMessage()));
        }
    }

    public void moveEntry(SRMUser user, String from, String to)
        throws SRMException
    {
        Subject subject = Subjects.getSubject((AuthorizationRecord) user);
        PnfsHandler handler = new PnfsHandler(_pnfs, subject);
        String fromPath = getFullPath(from);
        String toPath = getFullPath(to);

        try {
            try {
                FileAttributes attr =
                    handler.getFileAttributes(toPath, EnumSet.of(FileAttribute.TYPE));
                if (attr.getFileType() != FileType.DIR) {
                    throw new SRMDuplicationException("Destination exists");
                }

                File fromFile = new File(fromPath);
                File toFile = new File(new File(toPath), fromFile.getName());
                toPath = toFile.toString();
                to = to + "/" + fromFile.getName();
            } catch (FileNotFoundCacheException e) {
                /* Destination name does not exist; not a problem.
                 */
            }

            /* In case the source and destination names are identical, we
             * silently ignore the request.
             */
            if (fromPath.equals(toPath)) {
                _log.info("moveEntry: Source and destination paths are identical: " + fromPath);
                return;
            }

            handler.renameEntry(fromPath, toPath, false);
        } catch (FileNotFoundCacheException e) {
            throw new SRMInvalidPathException("No such file or directory", e);
        } catch (FileExistsCacheException e) {
            throw new SRMDuplicationException("Destination exists", e);
        } catch (NotDirCacheException e) {
            /* The parent of the target name did not exist or was not
             * a directory.
             */
            String parent = (new File(to)).getParent();
            throw new SRMInvalidPathException("No such directory: " +
                                              parent, e);
        } catch (PermissionDeniedCacheException e) {
            throw new SRMException("Permission denied");
        } catch (TimeoutCacheException e) {
            _log.error("Failed to rename " + fromPath + " due to timeout");
            throw new SRMInternalErrorException("Internal name space timeout");
        } catch (CacheException e) {
            _log.error("Failed to rename " + fromPath + ": " + e.getMessage());
            throw new SRMException(String.format("Rename failed [rc=%d,msg=%s]",
                                                 e.getRc(), e.getMessage()));
        }
    }

    public boolean canRead(SRMUser user, String fileId, FileMetaData fmd) {
        return _canRead(user,fileId,fmd);
    }

    public static boolean _canRead(SRMUser user, String fileId, FileMetaData fmd) {
        PnfsId pnfsId = new PnfsId(fileId);
        int uid = Integer.parseInt(fmd.owner);
        int gid = Integer.parseInt(fmd.group);
        int permissions = fmd.permMode;

        if(pnfsId == null) {
            return false;
        }

        if(permissions == 0 ) {
            return false;
        }

        if(Permissions.worldCanRead(permissions)) {
            return true;
        }

        if(uid == -1 || gid == -1) {
            return false;
        }

        if(user == null || (!(user instanceof AuthorizationRecord))) {
            return false;
        }
        AuthorizationRecord duser = (AuthorizationRecord) user;

        if(duser.getGid() == gid && Permissions.groupCanRead(permissions)) {
            return true;
        }

        if(duser.getUid() == uid && Permissions.userCanRead(permissions)) {
            return true;
        }

        return false;


    }

    // To do:  extract common functionality from this and _canRead
    //         into another method.
    public boolean canRead(SRMUser user, FileMetaData fmd) {
        int uid = Integer.parseInt(fmd.owner);
        int gid = Integer.parseInt(fmd.group);
        int permissions = fmd.permMode;

        if(permissions == 0 ) {
            return false;
        }

        if(Permissions.worldCanRead(permissions)) {
            return true;
        }

        if(uid == -1 || gid == -1) {
            return false;
        }

        if(user == null || (!(user instanceof AuthorizationRecord))) {
            return false;
        }
        AuthorizationRecord duser = (AuthorizationRecord) user;

        if(duser.getGid() == gid && Permissions.groupCanRead(permissions)) {
            return true;
        }

        if(duser.getUid() == uid && Permissions.userCanRead(permissions)) {
            return true;
        }

        return false;
    }



    public boolean canWrite(SRMUser user, String fileId, FileMetaData fmd,
        String parentFileId, FileMetaData parentFmd, boolean overwrite) {
        return _canWrite(user,fileId,fmd,parentFileId,parentFmd,overwrite);
    }

    public static boolean _canWrite(SRMUser user,
            String fileId,
            FileMetaData fmd,
            String parentFileId,
            FileMetaData parentFmd,
            boolean overwrite) {
        // we can not overwrite file in dcache (at least for now)
        if(! overwrite) {
            if(fileId != null ) {
                // file exists and we can't overwrite
                return false;
            }
        }

        if( parentFileId == null) {
            return false;
        }

        AuthorizationRecord duser = (AuthorizationRecord) user;
        boolean canWrite;
        if(fileId == null) {
            canWrite = true;
        } else {
            int uid = Integer.parseInt(fmd.owner);
            int gid = Integer.parseInt(fmd.group);
            int permissions = fmd.permMode;

            if(permissions == 0 ) {
               canWrite = false;
            } else if(Permissions.worldCanWrite(permissions) ) {
               canWrite = true;
            } else if(uid == -1 || gid == -1) {
               canWrite = false;
            } else  if(user == null || (!(user instanceof AuthorizationRecord))) {
               canWrite = false;
            } else  if(duser.getGid() == gid &&
                    Permissions.groupCanWrite(permissions) ) {
                canWrite = true;
            } else  if(duser.getUid() == uid &&
                    Permissions.userCanWrite(permissions)) {
                canWrite = true;
            } else {
                canWrite = false;
            }
        }

        int parentUid = Integer.parseInt(parentFmd.owner);
        int parentGid = Integer.parseInt(parentFmd.group);
        int parentPermissions = parentFmd.permMode;

        boolean parentCanWrite;
        if(parentPermissions == 0 ) {
           parentCanWrite = false;
        } else if(Permissions.worldCanWrite(parentPermissions) &&
                Permissions.worldCanExecute(parentPermissions)) {
           parentCanWrite = true;
        } else if(parentUid == -1 || parentGid == -1) {
           parentCanWrite = false;
        } else  if(user == null || (!(user instanceof AuthorizationRecord))) {
           parentCanWrite = false;
        } else  if(duser.getGid() == parentGid &&
                Permissions.groupCanWrite(parentPermissions) &&
                Permissions.groupCanExecute(parentPermissions)) {
            parentCanWrite = true;
        } else  if(duser.getUid() == parentUid &&
                Permissions.userCanWrite(parentPermissions) &&
                Permissions.userCanExecute(parentPermissions)) {
            parentCanWrite = true;
        } else {
            parentCanWrite = false;
        }
        return canWrite && parentCanWrite;

    }

    public static boolean _canDelete(SRMUser user, String fileId,
        FileMetaData fmd) {
        // we can not overwrite file in dcache (at least for now)
        if(fileId == null ) {
            return false;
        }

        PnfsId pnfsId = new PnfsId(fileId);
        int uid = Integer.parseInt(fmd.owner);
        int gid = Integer.parseInt(fmd.group);
        int permissions = fmd.permMode;

        if(permissions == 0 ) {
            return false;
        }

        if(Permissions.worldCanWrite(permissions)) {
            return true;
        }

        if(uid == -1 || gid == -1) {
            return false;
        }

        if(user == null || (!(user instanceof AuthorizationRecord))) {
            return false;
        }
        AuthorizationRecord duser = (AuthorizationRecord) user;


        if(duser.getGid() == gid &&
                Permissions.groupCanWrite(permissions) ) {
            return true;
        }

        if(duser.getUid() == uid &&
                Permissions.userCanWrite(permissions) ) {
            return true;
        }

        return false;


    }

    /**
     * @param user User ID
     * @param remoteTURL
     * @param actualFilePath
     * @param remoteUser
     * @param remoteCredetial
     * @param callbacks
     * @throws SRMException
     * @return copy handler id
     */
    public String getFromRemoteTURL(SRMUser user,
            String remoteTURL,
            String actualFilePath,
            SRMUser remoteUser,
            Long remoteCredentialId,
            String spaceReservationId,
            long size,
            CopyCallbacks callbacks) throws SRMException{
        actualFilePath = srm_root+"/"+actualFilePath;
        _log.debug(" getFromRemoteTURL from "+remoteTURL+" to " +actualFilePath);
        return performRemoteTransfer(user,remoteTURL,actualFilePath,true,
                remoteUser,
                remoteCredentialId,
                spaceReservationId,
                Long.valueOf(size),
                callbacks);

    }

    public String getFromRemoteTURL(SRMUser user,
            String remoteTURL,
            String actualFilePath,
            SRMUser remoteUser,
            Long remoteCredentialId,
            CopyCallbacks callbacks) throws SRMException{
        actualFilePath = srm_root+"/"+actualFilePath;
        _log.debug(" getFromRemoteTURL from "+remoteTURL+" to " +actualFilePath);
        return performRemoteTransfer(user,remoteTURL,actualFilePath,true,
                remoteUser,
                remoteCredentialId,
                null,
                null,
                callbacks);

    }

    /**
     * @param user
     * @param filePath
     * @param remoteTURL
     * @param remoteUser
     * @param remoteCredetial
     * @param callbacks
     * @throws SRMException
     * @return copy handler id
     */
    public String putToRemoteTURL(SRMUser user,
            String filePath,
            String remoteTURL,
            SRMUser remoteUser,
            Long remoteCredentialId,
            CopyCallbacks callbacks)
            throws SRMException{
        String actualFilePath = srm_root+"/"+filePath;
        _log.debug(" putToRemoteTURL from "+actualFilePath+" to " +remoteTURL);
        return performRemoteTransfer(user,remoteTURL,actualFilePath,false,
                remoteUser,
                remoteCredentialId,
                null,
                null,
                callbacks);


    }

    public void killRemoteTransfer(String transferId) {

        try {
            long callerId = Long.parseLong(transferId);
            Long longCallerId = Long.valueOf(callerId);
            TransferInfo info;
            synchronized(callerIdToHandler) {
                Object o =  callerIdToHandler.get(longCallerId);
                if( o == null) {
                    return;
                }
                info = (TransferInfo) o;
            }
            CancelTransferMessage cancel =
                    new diskCacheV111.vehicles.transferManager.
                CancelTransferMessage(info.transferId, callerId);
            sendMessage(new CellMessage(info.cellPath,cancel));
        } catch (NoRouteToCellException e) {
            _log.error("Failed to kill remote transfer: " + e.getMessage());
        } catch (NumberFormatException e) {
            _log.error("Failed to kill remote transfer: Cannot parse transfer ID");
        }
    }


    private String performRemoteTransfer(
            SRMUser user,
            String remoteTURL,
            String actualFilePath,
            boolean store,
            SRMUser remoteUser,
            Long remoteCredentialId,
            String spaceReservationId,
            Long size,
            CopyCallbacks callbacks
            )
            throws SRMException {
        _log.debug("performRemoteTransfer performing "+(store?"store":"restore"));
        if(!verifyUserPathIsRootSubpath(actualFilePath,user)) {
            throw new SRMAuthorizationException("user's path "+actualFilePath+
                    " is not subpath of the user's root");
        }

        if(remoteTURL.startsWith("gsiftp://")) {
            AuthorizationRecord duser = (AuthorizationRecord)user;

            //call this for the sake of checking that user is reading
            // from the "root" of the user
            String path = getTurlPath(actualFilePath,"gsiftp",user);
            if(path == null) {
                throw new SRMException("user is not authorized to access path: "+
                        actualFilePath);
            }


            RemoteGsiftpTransferManagerMessage gsiftpTransferRequest;

            // if space reservation was performed for a file of known size

            RequestCredential remoteCredential =
                RequestCredential.getRequestCredential(remoteCredentialId);
            String credentialName = "Unknown";
            if (remoteCredential != null)
            	credentialName = remoteCredential.getCredentialName();

            if(store && spaceReservationId != null  && size != null) {
                gsiftpTransferRequest = new RemoteGsiftpTransferManagerMessage(
                        duser.getName(),
                        duser.getUid(),
                        duser.getGid(),
                        remoteTURL,
                        actualFilePath,
                        store,
                        remoteCredentialId,
                        credentialName,
                        config.getBuffer_size(),
                        config.getTcp_buffer_size(),
                        spaceReservationId,
                        config.isSpace_reservation_strict(),
                        size
                        );
            } else {
                gsiftpTransferRequest = new RemoteGsiftpTransferManagerMessage(
                        duser.getName(),
                        duser.getUid(),
                        duser.getGid(),
                        remoteTURL,
                        actualFilePath,
                        store,
                        remoteCredentialId,
                        credentialName,
                        config.getBuffer_size(),
                        config.getTcp_buffer_size()
                        );
            }
            gsiftpTransferRequest.setReplyRequired(true);
            gsiftpTransferRequest.setStreams_num(config.getParallel_streams());
            CellMessage answer;
            try {
                CellPath cellPath = new CellPath(remoteGridftpTransferManagerName);
                answer = sendAndWait(new CellMessage(
                        cellPath,
                        gsiftpTransferRequest),1000*60*60*24);
                RemoteGsiftpTransferManagerMessage
                        answer_message =
                        (RemoteGsiftpTransferManagerMessage)
                        answer.getMessageObject();
                if(answer_message.getReturnCode() != 0) {
                    if(answer_message.getErrorObject() != null) {
                        throw new SRMException("TransferManager error"+
                                answer_message.getErrorObject().toString());
                    } else {
                        throw new SRMException("TransferManager error");

                    }
                }
                long id = answer_message.getId();
                _log.debug("received first RemoteGsiftpTransferManagerMessage reply from" +
                    " transfer manager, id ="+id);
                GridftpTransferInfo info = new GridftpTransferInfo(id,
                    remoteCredentialId,callbacks,cellPath);
                Long longCallerId = Long.valueOf(id);
                _log.debug("strorring info for callerId = "+longCallerId);
                synchronized(callerIdToHandler) {
                    callerIdToHandler.put(longCallerId,info);
                }
                return longCallerId.toString();
            } catch (NoRouteToCellException e) {
                throw new SRMException("Transfer manager is unavailable: " +
                                       e.getMessage(), e);
            } catch (InterruptedException e) {
                throw new SRMException("Request to transfer manager got interruptd", e);
            }
        }
        throw new SRMException("not implemented");
    }

    private Map callerIdToHandler = new HashMap();

    private class TransferInfo {
        private long transferId;
        private CopyCallbacks callbacks;
        private CellPath cellPath;

        public TransferInfo(long transferId,CopyCallbacks callbacks,
            CellPath cellPath ) {
            this.transferId = transferId;
            this.callbacks = callbacks;
            this.cellPath = cellPath;
        }
    }

    private class GridftpTransferInfo extends TransferInfo {
        private Long remoteCredentialId;

        public GridftpTransferInfo(long transferId,Long remoteCredentialId,
            CopyCallbacks callbacks,CellPath cellPath) {
            super( transferId,callbacks,cellPath);
            this.remoteCredentialId = remoteCredentialId;
        }
    }

    private void handleTransferManagerMessage(TransferManagerMessage message) {
        Long callerId = Long.valueOf(message.getId());
        _log.debug("handleTransferManagerMessage for callerId="+callerId);

        if(message instanceof RemoteGsiftpDelegateUserCredentialsMessage) {
            RemoteGsiftpDelegateUserCredentialsMessage delegate =
                    (RemoteGsiftpDelegateUserCredentialsMessage)message;
            Long remoteCredentialId = delegate.getRequestCredentialId();
            final String host = delegate.getHost();
            final int port = delegate.getPort();
            RequestCredential remoteCredential =
                    RequestCredential.getRequestCredential(remoteCredentialId);
            if(remoteCredential != null) {
                final  GSSCredential gssRemoteCredential =
                    remoteCredential.getDelegatedCredential();
                getNucleus().newThread(new Runnable() {public void run() {
                    delegate(gssRemoteCredential,host,port);
                }}, "credentialDelegator" ).start() ;
            } else {
                getNucleus().newThread(new Runnable() {public void run() {
                    delegate(null,host,port);
                }}, "credentialDelegator" ).start() ;
            }
            return;

        }

        Object o;
        synchronized(callerIdToHandler) {
            o = callerIdToHandler.get(callerId);
            if(o == null) {
                _log.error("TransferInfo for callerId="+callerId+"not found");
                return;
            }
        }
        TransferInfo info = (TransferInfo)o;

        if (message instanceof TransferCompleteMessage ) {
            TransferCompleteMessage complete =
                    (TransferCompleteMessage)message;
            info.callbacks.copyComplete(null,null);
            _log.debug("removing TransferInfo for callerId="+callerId);
            synchronized(callerIdToHandler) {
                callerIdToHandler.remove(callerId);
            }
        } else if(message instanceof TransferFailedMessage) {
            Object error =                 message.getErrorObject();
            if(error != null ) {
                if(error instanceof Exception) {
                    info.callbacks.copyFailed((Exception)error);
                } else {
                    info.callbacks.copyFailed(new CacheException(error.toString()));
                }

            } else {
                info.callbacks.copyFailed(new CacheException("transfer failed: "+
                    message.toString()));
            }

            _log.debug("removing TransferInfo for callerId="+callerId);
            synchronized(callerIdToHandler) {
                callerIdToHandler.remove(callerId);
            }

        }

    }

    private void delegate(GSSCredential credential, String host, int port) {
        if(credential == null) {
            _log.warn("cannot delegate,  user credential is null");
            try {
                //we can not deligate so we make this fail on the door side
                Socket s = new Socket(host,port);
                OutputStream sout= s.getOutputStream();
                sout.close();
                s.close();
            } catch(IOException ioe) {
                _log.error(ioe);
            }
        } else {
            try {
                String credname = credential.getName().toString();
                _log.info("SRMCell.Delegator, delegating credentials :"+
                        credential+   " to mover at "+host+
                        " listening on port "+port);

            }catch(org.ietf.jgss.GSSException gsse) {
                _log.error("invalid credentials :" + gsse.getMessage());
                try {
                    Socket s = new Socket(host,port);
                    OutputStream sout= s.getOutputStream();
                    sout.close();
                    s.close();
                } catch(IOException ioe) {
                    _log.error(ioe);
                }
                return;
            }

            try {
                SslGsiSocketFactory.delegateCredential(
                        InetAddress.getByName(host),
                        port,
                        credential,false);
                _log.info("delegation appears to have succeeded");
            } catch (RuntimeException e) {
                _log.fatal("delegation failed", e);
            } catch (Exception e) {
                _log.error("delegation failed: " + e.getMessage());
            }
        }
    }

    /**
     * Reserves spaceSize bytes of the space for
     * storage of file with the path filename
     * for future transfer from the host. <br>
     * The storage will invoke methods of the callback interface
     * to syncronously notify of the srm about the results of the reservation.
     * @param user User ID
     * @param spaceSize size of the space to be released
     * @see org.dcache.srm.ReserveSpaceCallbacks
     */
    public void reserveSpace(SRMUser user,
            long spaceSize,
            long reservationLifetime,
            String filename,
            String host,
            ReserveSpaceCallbacks callbacks){
        AuthorizationRecord duser = (AuthorizationRecord) user;
        String absolute_path = srm_root+"/"+filename;
        ReserveSpaceCompanion.reserveSpace(duser,
                absolute_path,
                callbacks,
                host,
                spaceSize,
                reservationLifetime,
                this);

    }


    /**
     * Release spaceSize bytes of the reserved space identified with the token
     * This method returns via callbacks the size of the
     * space released in the "pool" (pool is a space storage part that can be
     * utilized in a continuous manner. In case of dcache pool is a
     * dcache pool) and the name (unique string  id) of the pool.
     * @param user User ID
     * @param spaceSize size of the space to be released
     * @param spaceToken identifier of the space
     * @param callbacks This interface is used for
     * asyncronous notification of SRM of the
     * various actions performed to release space in the storage
     */
    public void releaseSpace( SRMUser user,
        long spaceSize,
        String spaceToken,
        ReleaseSpaceCallbacks callbacks){
        AuthorizationRecord duser = (AuthorizationRecord) user;
        ReleaseSpaceCompanion.releaseSpace(spaceToken,spaceSize,callbacks,this);

    }

    /**
     * Release all of the space identified with the token
     * This method returns via callbacks the size of the
     * space released in the "pool" (pool is a space storage part that can be
     * utilized in a continuous manner. In case of dcache pool is a
     * dcache pool) and the name (unique string  id) of the pool.
     * @param user User ID
     * @param reservationToken identifier of the space
     * @param callbacks This interface is used for asyncronous notification of SRM of the
     * various actions performed to release space in the storage
     */
    public void releaseSpace( SRMUser user,  String spaceToken,
        ReleaseSpaceCallbacks callbacks){
        AuthorizationRecord duser = (AuthorizationRecord) user;
        ReleaseSpaceCompanion.releaseSpace(spaceToken,callbacks,this);

    }


    private volatile StorageElementInfo storageElementInfo =
        new StorageElementInfo();

    public StorageElementInfo getStorageElementInfo(SRMUser user)
    {
        return storageElementInfo;
    }

    private StorageElementInfo getStorageElementInfo()
    {
        return storageElementInfo;
    }

    private List pools = Collections.emptyList();

    private void updateStorageElementInfo() throws SRMException      {
        try{
            PoolManagerGetPoolListMessage getPoolsQuery = new
                    PoolManagerGetPoolListMessage();
            getPoolsQuery.setReplyRequired(true);
            CellMessage getPoolMessage = new CellMessage( _poolMgrPath , getPoolsQuery ) ;
            getPoolMessage = sendAndWait(  getPoolMessage ,
                    __poolManagerTimeout*1000 ) ;

            if(getPoolMessage != null) {
                Object o = getPoolMessage.getMessageObject();
                if(o != null && o instanceof  PoolManagerGetPoolListMessage) {
                    getPoolsQuery = (PoolManagerGetPoolListMessage)o;

                    StorageElementInfo info = new StorageElementInfo();
                    List newPools = getPoolsQuery.getPoolList();
                    if(!newPools.isEmpty() ) {
                        pools = newPools;
                    } else {
                        _log.info("receieved an empty pool list from the pool manager," +
                            "using the prevois list");
                    }
                } else {
                    _log.error("poolManager returned o="+o+
                        ", using previosly saved pool list");
                }
            } else {
                _log.error("poolManager timeout expired, using previosly saved pool list");
            }
        } catch (NoRouteToCellException e) {
            _log.error("PoolManager is unavailable: " + e.getMessage());
        } catch (InterruptedException e) {
            _log.error("Request to PoolManager got interrupted");
        }

        StorageElementInfo info = new StorageElementInfo();
        for( Iterator i = pools.iterator(); i.hasNext();) {
            try {
                StorageElementInfo poolInfo = getPoolInfo((String)(i.next()));
                info.availableSpace += poolInfo.availableSpace;
                info.totalSpace += poolInfo.totalSpace;
                info.usedSpace += poolInfo.usedSpace;
            } catch (RuntimeException e) {
                _log.fatal("can not get info from pool "+i.next()+
                           " , contunue with the rest of the pools", e);
            } catch (Exception e) {
                _log.error("can not get info from pool "+i.next()+
                           ", contunue with the rest of the pools: " +
                           e.getMessage());
            }
        }

        storageElementInfo = info;
    }



    /**
     * we use run method to update the storage info structure periodically
     */
    public void run() {
        while(true) {
            try {
                updateStorageElementInfo();
            } catch(SRMException srme){
                _log.warn(srme);
            }
            try {
                Thread.sleep(config.getStorage_info_update_period());
            } catch(InterruptedException ie) {
                return;
            }
        }
    }

    /**
     * Provides a directory listing of directoryName if and only if
     * directoryName is no a symbolic link. As a side effect, the
     * method checks that directoryName can be deleted by the user.
     *
     * @param user The SRMUser performing the operation; this myst be
     * of type AuthorizationRecord
     * @param directoryName The directory to delete
     * @return The array of directory entries or null if directoryName
     * is a symbolic link
     */
    public String[] listNonLinkedDirectory(SRMUser user, String directoryName)
        throws SRMException
    {
        AuthorizationRecord duser = (AuthorizationRecord) user;
        String path = getFullPath(directoryName);
        try {
            boolean canDelete =
                permissionHandler.canDeleteDir(duser.getUid(),
                                               duser.getGid(),
                                               path);
            if (!canDelete) {
                _log.warn("Cannot delete directory " + path +
                          ": Permission denied");
                throw new SRMAuthorizationException("Permission denied");
            }

            FileAttributes attr =
                _pnfs.getFileAttributes(path, EnumSet.of(FileAttribute.TYPE));
            if (attr.getFileType() == FileType.LINK)  {
                return null;
            }
        } catch (FileNotFoundCacheException e) {
            throw new SRMInvalidPathException("No such file or directory", e);
        } catch (NotInTrashCacheException e) {
            throw new SRMInvalidPathException("No such file or directory", e);
        } catch (TimeoutCacheException e) {
            throw new SRMInternalErrorException("Internal name space timeout", e);
        } catch (CacheException e) {
            _log.error("Failed to list directory " + path + ": "
                       + e.getMessage());
            throw new SRMException(String.format("Failed delete directory [rc=%d,msg=%s]",
                                                 e.getRc(), e.getMessage()));
        }

        return listDirectory(user, directoryName, null);
    }

    public File[] listDirectoryFiles(SRMUser user, String directoryName,
                                     FileMetaData fileMetaData)
        throws SRMException
    {
        final File path = new File(getFullPath(directoryName));
        final Collection<File> result = new ArrayList<File>();
        Subject subject = Subjects.getSubject((AuthorizationRecord) user);
        DirectoryListPrinter printer =
            new DirectoryListPrinter()
            {
                public Set<FileAttribute> getRequiredAttributes()
                {
                    return EnumSet.noneOf(FileAttribute.class);
                }

                public void print(FileAttributes dirAttr, DirectoryEntry entry)
                {
                    result.add(new File(path, entry.getName()));
                }
            };

        try {
            _listSource.printDirectory(subject, printer, path, null, null);
            return result.toArray(new File[0]);
        } catch (TimeoutCacheException e) {
            throw new SRMInternalErrorException("Internal name space timeout", e);
        } catch (InterruptedException e) {
            throw new SRMInternalErrorException("List aborted by administrator", e);
        } catch (NotDirCacheException e) {
            throw new SRMInvalidPathException("Not a directory", e);
        } catch (FileNotFoundCacheException e) {
            throw new SRMInvalidPathException("No such file or directory", e);
        } catch (NotInTrashCacheException e) {
            throw new SRMInvalidPathException("No such file or directory", e);
        } catch (PermissionDeniedCacheException e) {
            throw new SRMAuthorizationException("Permission denied", e);
        } catch (CacheException e) {
            _log.error("Failed to list directory " + path + ": "
                       + e.getMessage());
            throw new SRMException(String.format("List failed [rc=%d,msg=%s]",
                                                 e.getRc(), e.getMessage()));
        }
    }

    public String[] listDirectory(SRMUser user, String directoryName,
                                  FileMetaData fileMetaData)
        throws SRMException
    {
        final File path = new File(getFullPath(directoryName));
        final Collection<String> result = new ArrayList<String>();
        Subject subject = Subjects.getSubject((AuthorizationRecord) user);
        DirectoryListPrinter printer =
            new DirectoryListPrinter()
            {
                public Set<FileAttribute> getRequiredAttributes()
                {
                    return EnumSet.noneOf(FileAttribute.class);
                }

                public void print(FileAttributes dirAttr, DirectoryEntry entry)
                {
                    result.add(entry.getName());
                }
            };

        try {
            _listSource.printDirectory(subject, printer, path, null, null);
            return result.toArray(new String[0]);
        } catch (TimeoutCacheException e) {
            throw new SRMInternalErrorException("Internal name space timeout", e);
        } catch (InterruptedException e) {
            throw new SRMInternalErrorException("List aborted by administrator", e);
        } catch (NotDirCacheException e) {
            throw new SRMInvalidPathException("Not a directory", e);
        } catch (FileNotFoundCacheException e) {
            throw new SRMInvalidPathException("No such file or directory", e);
        } catch (NotInTrashCacheException e) {
            throw new SRMInvalidPathException("No such file or directory", e);
        } catch (PermissionDeniedCacheException e) {
            throw new SRMAuthorizationException("Permission denied", e);
        } catch (CacheException e) {
            throw new SRMException(String.format("List failed [rc=%d,msg=%s]",
                                                 e.getRc(), e.getMessage()));
        }
    }

    public void srmReserveSpace(SRMUser user,
            long sizeInBytes,
            long spaceReservationLifetime,
            String retentionPolicy,
            String accessLatency,
            String description,
            SrmReserveSpaceCallbacks callbacks) {
        AuthorizationRecord duser = (AuthorizationRecord) user;

        SrmReserveSpaceCompanion.reserveSpace(
                duser,
                sizeInBytes,
                spaceReservationLifetime,
                retentionPolicy,
                accessLatency,
                description,
                callbacks,
                this);
    }


    public void srmReleaseSpace(SRMUser user,
            String spaceToken,
            Long releaseSizeInBytes, // everything is null
            SrmReleaseSpaceCallbacks callbacks) {
        long longSpaceToken;
        try {
            longSpaceToken = Long.parseLong(spaceToken);
        } catch(Exception e){
            callbacks.ReleaseSpaceFailed("invalid space token="+spaceToken);
            return;
        }

        AuthorizationRecord duser = (AuthorizationRecord) user;
        SrmReleaseSpaceCompanion.releaseSpace(duser,
                longSpaceToken,
                releaseSizeInBytes,
                callbacks,
                this);
    }

    public void srmMarkSpaceAsBeingUsed(SRMUser user,
            String spaceToken,
            String fileName,
            long sizeInBytes,
            long useLifetime,
            boolean overwrite,
            SrmUseSpaceCallbacks callbacks) {
        long longSpaceToken;
        try {
            longSpaceToken = Long.parseLong(spaceToken);
        } catch(Exception e){
            callbacks.SrmUseSpaceFailed("invalid space token="+spaceToken);
            return;
        }

        AuthorizationRecord duser = (AuthorizationRecord) user;
        String actualFilePath = srm_root+"/"+fileName;
        FsPath fsPath           = new FsPath(actualFilePath);
        SrmMarkSpaceAsBeingUsedCompanion.markSpace(
                duser,
                longSpaceToken,
                fsPath.toString(),
                sizeInBytes,
                useLifetime,
                overwrite,
                callbacks,
                this);
    }

    public void srmUnmarkSpaceAsBeingUsed(
            SRMUser user,
            String spaceToken,
            String fileName,
            SrmCancelUseOfSpaceCallbacks callbacks) {
        long longSpaceToken;
        try {
            longSpaceToken = Long.parseLong(spaceToken);
        } catch(Exception e){
            callbacks.CancelUseOfSpaceFailed("invalid space token="+spaceToken);
            return;
        }
        AuthorizationRecord duser = (AuthorizationRecord) user;
        String actualFilePath = srm_root+"/"+fileName;
        FsPath fsPath           = new FsPath(actualFilePath);
        SrmUnmarkSpaceAsBeingUsedCompanion.unmarkSpace(
                duser,
                longSpaceToken,
                fsPath.toString(),
                callbacks,
                this);
    }

    public final class LoginBrokerHandler implements Runnable
    {
        private String _srmLoginBroker        = null ;
        private String _protocolFamily     = null ;
        private String _protocolVersion    = null ;
        private long   _brokerUpdateTime   = 5*60*1000 ;
        private double _brokerUpdateOffset = (double)0.1 ;
        private LoginBrokerInfo _info      = null ;
        private double _currentLoad        = 0.0 ;
        private LoginBrokerHandler(){

            _srmLoginBroker = _args.getOpt( "srmLoginBroker" ) ;
            if( _srmLoginBroker == null )return;

            _protocolFamily    = _args.getOpt("protocolFamily" ) ;
            if( _protocolFamily == null )_protocolFamily = "SRM" ;
            _protocolVersion = _args.getOpt("protocolVersion") ;
            if( _protocolVersion == null )_protocolVersion = "0.1" ;
            String tmp = _args.getOpt("brokerUpdateTime") ;
            if(tmp != null) {
                try{
                    _brokerUpdateTime = Long.parseLong(tmp) * 1000 ;
                } catch (NumberFormatException e) {
                    _log.fatal("Failed to parse brokerUpdateTime: " +
                               e.getMessage());
                }
            }
            tmp = _args.getOpt("brokerUpdateOffset") ;
            if(tmp != null) {
                try{
                    _brokerUpdateOffset = Double.parseDouble(tmp) ;
                }catch(NumberFormatException e ){
                    _log.fatal("Failed to parse brokerUpdateOffset: " +
                               e.getMessage());
                }
            }

            _info = new LoginBrokerInfo(
                    getNucleus().getCellName() ,
                    getNucleus().getCellDomainName() ,
                    _protocolFamily ,
                    _protocolVersion ,
                    Storage.this.getClass().getName() ) ;

            _info.setUpdateTime( _brokerUpdateTime ) ;

            getNucleus().newThread( this , "loginBrokerHandler" ).start() ;

        }
        public void run(){
            try{
                synchronized(this){
                    while( ! Thread.interrupted() ){
                        try {
                            runUpdate() ;
                        } catch (RuntimeException e) {
                            _log.fatal("Login Broker Thread reports : ", e);
                        }
                        wait( _brokerUpdateTime ) ;
                    }
                }
            }catch( Exception io ){
                _log.warn( "Login Broker Thread terminated due to "+io ) ;
            }
        }

        public String hh_lb_set_update = "<updateTime/sec>" ;
        public String ac_lb_set_update_$_1( Args args ){
            long update = Long.parseLong( args.argv(0) )*1000 ;
            if( update < 2000 )
                throw new
                        IllegalArgumentException("Update time out of range") ;

            synchronized(this){
                _brokerUpdateTime = update ;
                _info.setUpdateTime(update) ;
                notifyAll() ;
            }
            return "" ;
        }
        private synchronized void runUpdate(){

            _info.setHosts(_hosts);
            _info.setPort(config.getPort());
            if(srm != null) {
                _currentLoad = srm.getLoad();
            }
            _info.setLoad(_currentLoad);
            try{
                sendMessage(new CellMessage(new CellPath(_srmLoginBroker),_info));
            } catch (NoRouteToCellException e) {
                _log.error("Login broker is unavailable: " + e.getMessage());
            }
        }

        public void getInfo( PrintWriter pw ){
            if( _srmLoginBroker == null ){
                pw.println( "    Login Broker : DISABLED" ) ;
                return ;
            }
            pw.println( "    LoginBroker      : "+_srmLoginBroker ) ;
            pw.println( "    Protocol Family  : "+_protocolFamily ) ;
            pw.println( "    Protocol Version : "+_protocolVersion ) ;
            pw.println( "    Update Time      : "+
                (_brokerUpdateTime/1000)+" seconds" ) ;
            pw.println( "    Update Offset    : "+
                    ((int)(_brokerUpdateOffset*100.))+" %" ) ;

        }
        private boolean isActive(){ return _srmLoginBroker != null ; }
    }

    /**
     *
     * @param spaceTokens
     * @throws org.dcache.srm.SRMException
     * @return
     */
    public TMetaDataSpace[] srmGetSpaceMetaData(SRMUser user,
        String[] spaceTokens)
    throws SRMException {
        _log.debug("srmGetSpaceMetaData");
        if(spaceTokens == null) {
            throw new SRMException("null array of space tokens");
        }
        long[] tokens = new long[spaceTokens.length];
        for(int i = 0; i<spaceTokens.length; ++i) {
            try{
                tokens[i] = Long.parseLong(spaceTokens[i]);
            } catch (Exception e) {
                throw new SRMException("invalid token: "+spaceTokens[i]);
            }
        }
        GetSpaceMetaData getSpaces = new GetSpaceMetaData(tokens);
        CellMessage cellMessage = new CellMessage(
                new CellPath("SrmSpaceManager"),
                getSpaces);
        try {
            cellMessage = sendAndWait(
                    cellMessage,
                    3*60*1000);
        } catch (NoRouteToCellException e) {
            throw new SRMException("SrmSpaceManager is unavailable: " +
                                   e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new SRMException("Request to SrmSpaceManaget got interrupted", e);
        }

        if(cellMessage == null ||
                    cellMessage.getMessageObject() ==null ||
                    !(cellMessage.getMessageObject()  instanceof GetSpaceMetaData)) {
            String error = "sent GetSpaceMetaData to SrmSpaceManager, received "+
                    (cellMessage == null?"timeout":cellMessage.getMessageObject())
                    +" back";
            _log.error(error );
                throw new SRMException(error);
        }
        getSpaces = (GetSpaceMetaData)cellMessage.getMessageObject() ;
        if(getSpaces.getReturnCode() != 0) {
            _log.warn("GetSpaceMetaData failed with rc="+getSpaces.getReturnCode()+
                " error="+getSpaces.getErrorObject());
            throw new SRMException("GetSpaceMetaData failed with rc="+
                getSpaces.getReturnCode()+ " error="+getSpaces.getErrorObject());
        }

        diskCacheV111.services.space.Space[] spaces = getSpaces.getSpaces();
        tokens =  getSpaces.getSpaceTokens();
        TMetaDataSpace[] spaceMetaDatas = new TMetaDataSpace[spaces.length];
        for(int i = 0; i<spaceMetaDatas.length; ++i){
            if(spaces[i] != null) {
                diskCacheV111.services.space.Space space = spaces[i];
                spaceMetaDatas[i] = new TMetaDataSpace();
                spaceMetaDatas[i].setSpaceToken(Long.toString(space.getId()));
                long lifetime = space.getLifetime();
                int lifetimeleft;
                if ( lifetime == -1) {  // -1 corresponds to infinite lifetime
                    lifetimeleft = -1;
                    spaceMetaDatas[i].setLifetimeAssigned(Integer.valueOf(-1));
                    spaceMetaDatas[i].setLifetimeLeft(Integer.valueOf(-1));
                } else {
			lifetimeleft = (int)((space.getCreationTime()+lifetime - System.currentTimeMillis())/1000);
                    lifetimeleft= lifetimeleft < 0? 0: lifetimeleft;
                    spaceMetaDatas[i].setLifetimeAssigned(Integer.valueOf((int)(lifetime/1000)));
                    spaceMetaDatas[i].setLifetimeLeft(Integer.valueOf(lifetimeleft));
                }
                TRetentionPolicy policy =
                    space.getRetentionPolicy().equals( RetentionPolicy.CUSTODIAL)?
                      TRetentionPolicy.CUSTODIAL :
                        space.getRetentionPolicy().equals(RetentionPolicy.OUTPUT)?
                            TRetentionPolicy.OUTPUT:TRetentionPolicy.REPLICA;
                TAccessLatency latency =
                    space.getAccessLatency().equals(AccessLatency.ONLINE) ?
                            TAccessLatency.ONLINE: TAccessLatency.NEARLINE;
                spaceMetaDatas[i].setRetentionPolicyInfo(
                    new TRetentionPolicyInfo(policy,latency));
                spaceMetaDatas[i].setTotalSize(
                    new org.apache.axis.types.UnsignedLong(
                        space.getSizeInBytes()));
                spaceMetaDatas[i].setGuaranteedSize(
                    spaceMetaDatas[i].getTotalSize());
                spaceMetaDatas[i].setUnusedSize(
                    new org.apache.axis.types.UnsignedLong(
                        space.getSizeInBytes() - space.getUsedSizeInBytes()));
                diskCacheV111.services.space.SpaceState spaceState =space.getState();
                if(diskCacheV111.services.space.SpaceState.RESERVED.equals(spaceState)) {
                    if(lifetimeleft == 0 ) {
                        spaceMetaDatas[i].setStatus(
                                new TReturnStatus(
                                TStatusCode.SRM_SPACE_LIFETIME_EXPIRED,"expired"));
                    }
                    else {
                        spaceMetaDatas[i].setStatus(
                                new TReturnStatus(TStatusCode.SRM_SUCCESS,"ok"));
                    }
                } else if(diskCacheV111.services.space.SpaceState.EXPIRED.equals(
                    spaceState)) {
                    spaceMetaDatas[i].setStatus(
                        new TReturnStatus(
                        TStatusCode.SRM_SPACE_LIFETIME_EXPIRED,"expired"));
                } else {
                    spaceMetaDatas[i].setStatus(
                            new TReturnStatus(TStatusCode.SRM_FAILURE,
                            "space has been released "));
                }
                spaceMetaDatas[i].setOwner("VoGroup="+space.getVoGroup()+"" +
                    " VoRole="+space.getVoRole());
            } else {
                spaceMetaDatas[i] = new TMetaDataSpace();
                spaceMetaDatas[i].setSpaceToken(Long.toString(tokens[i]));
                spaceMetaDatas[i].setStatus(new TReturnStatus(
						   TStatusCode.SRM_INVALID_REQUEST,"space not found"));
            }
        }
        return spaceMetaDatas;
    }

    /**
     *
     * @param description
     * @throws org.dcache.srm.SRMException
     * @return
     */
    public String[] srmGetSpaceTokens(SRMUser user, String description)
        throws SRMException {
        AuthorizationRecord duser = (AuthorizationRecord) user;
        _log.debug("srmGetSpaceTokens ("+description+")");
       GetSpaceTokens getTokens = new GetSpaceTokens(duser.getVoGroup(),
           duser.getVoRole(),description);
        CellMessage cellMessage = new CellMessage(
                new CellPath("SrmSpaceManager"),
                getTokens);
        try {
            cellMessage = sendAndWait(
                    cellMessage,
                    3*60*1000);
        } catch (NoRouteToCellException e) {
            throw new SRMException("SrmSpaceManager is unavailable: " +
                                   e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new SRMException("Request to SrmSpaceManager got interrupted", e);
        }
         if(cellMessage == null ||
            cellMessage.getMessageObject() ==null ||
            !(cellMessage.getMessageObject()  instanceof GetSpaceTokens)) {
            String error = "sent GetSpaceTokens to SrmSpaceManager, received "+
                    (cellMessage==null?"timeout":cellMessage.getMessageObject()) +" back";
            _log.error(error);
                throw new SRMException(error);
        }
        getTokens = (GetSpaceTokens)cellMessage.getMessageObject() ;
        if(getTokens.getReturnCode() != 0) {
            _log.warn("GetSpaceTokens failed with rc="+getTokens.getReturnCode()+
                " error="+getTokens.getErrorObject());
            throw new SRMException("GetSpaceTokens failed with rc="+
                getTokens.getReturnCode()+ " error="+getTokens.getErrorObject());
        }
       long tokens[] =  getTokens.getSpaceTokens();
       String tokenStrings[] =new String[tokens.length];
       for(int i =0; i<tokens.length; ++i) {
           tokenStrings[i] = Long.toString(tokens[i]);
           _log.debug("srmGetSpaceTokens returns token#"+i+" : "+tokenStrings[i]);
       }

       return tokenStrings;
    }

    public String[] srmGetRequestTokens(SRMUser user,String description)
        throws SRMException {
        try {
            Set<Long> tokens = srm.getBringOnlineRequestIds((SRMUser) user,
                    description);
            tokens.addAll(srm.getGetRequestIds((SRMUser) user,
                    description));
            tokens.addAll(srm.getPutRequestIds((SRMUser) user,
                    description));
            tokens.addAll(srm.getCopyRequestIds((SRMUser) user,
                    description));
            tokens.addAll(srm.getLsRequestIds((SRMUser) user,
                    description));
            Long[] tokenLongs = (Long[]) tokens.toArray(new Long[0]);
            String[] tokenStrings = new String[tokenLongs.length];
            for(int i=0;i<tokenLongs.length;++i) {
                tokenStrings[i] = tokenLongs[i].toString();
            }
            return tokenStrings;
        } catch (SQLException e) {
            _log.error("srmGetRequestTokens failed: " + e.getMessage());
            throw new SRMException("srmGetRequestTokens failed: " +
                                   e.getMessage(), e);
        }
    }

    /**
     *
     * we support only permanent file, lifetime is always -1
     * @param newLifetime SURL lifetime in seconds
     *   -1 stands for infinite lifetime
     * @return long lifetime left in seconds
     *   -1 stands for infinite lifetime
     *
     */
    public int srmExtendSurlLifetime(SRMUser user,
        String fileName, int newLifetime) throws SRMException {
        FileMetaData fmd = getFileMetaData(user,fileName);
        int uid = Integer.parseInt(fmd.owner);
        int gid = Integer.parseInt(fmd.group);
        int permissions = fmd.permMode;

        if(Permissions.worldCanWrite(permissions)) {
            return -1;
        }

        if(uid == -1 || gid == -1) {
            throw new SRMAuthorizationException(
                "User is not authorized to modify this file");
        }

        if(user == null || (!(user instanceof AuthorizationRecord))) {
            throw new SRMAuthorizationException(
                "User is not authorized to modify this file");
        }
        AuthorizationRecord duser = (AuthorizationRecord) user;

        if(duser.getGid() == gid && Permissions.groupCanWrite(permissions)) {
            return -1;
        }

        if(duser.getUid() == uid && Permissions.userCanWrite(permissions)) {
            return -1;
        }

        throw new SRMAuthorizationException(
            "User is not authorized to modify this file");

    }

    /**
     *
     *
     * @param user User ID
     * @param spaceToken of a valid space reservation
     * @param newReservationLifetime new lifetime
     * in millis to assign to space reservation
     * @return long lifetime of spacereservation left in milliseconds
     */
    public long srmExtendReservationLifetime(SRMUser user,
        String spaceToken, long newReservationLifetime) throws SRMException {
        long longSpaceToken;
        try {
            longSpaceToken = Long.parseLong(spaceToken);
        } catch (NumberFormatException e){
            throw new SRMException("Cannot parse space token: " +
                                   e.getMessage(), e);
        }
        ExtendLifetime extendLifetime =
                new ExtendLifetime( longSpaceToken, newReservationLifetime);

        try {
            CellMessage response =  sendAndWait( new CellMessage(
                    new CellPath( "SrmSpaceManager") ,
                    extendLifetime ) ,
                    60*60*1000
                    );
            if(response == null) {
                throw new SRMException("srmExtendReservationLifetime response " +
                    "lifetime expired");
            }
            extendLifetime = (ExtendLifetime) response.getMessageObject();
            //say("StageAndPinCompanion: recordAsPinned");
            //rr.recordAsPinned (_fr,true);
        } catch (NoRouteToCellException e) {
            throw new SRMException("SrmSpaceManager is unavailable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new SRMException("Request to SrmSpaceManager got interrupted", e);
        }

        if(extendLifetime.getReturnCode() != 0) {
            throw new SRMException("srmExtendReservationLifetime failed, " +
                "ExtendLifetime.returnCode="+
                    extendLifetime.getReturnCode()+" errorObject = "+
                extendLifetime.getErrorObject());
        }
        return extendLifetime.getNewLifetime();
    }

    /**
     *
     *
     * @param user User ID
     * @param pinId Id of a valid pin
     * @param newPinLifetime new lifetime in millis to assign to pin
     * @return long lifetime left for pin in millis
     */
    public long extendPinLifetime(SRMUser user,
        String fileId, String pinId, long newPinLifetime) throws SRMException {
        PnfsId pnfsId = null;
        try {
            pnfsId = new PnfsId(fileId);
        } catch (IllegalArgumentException e) {
            throw new SRMException("Invalid PNFS ID: " + fileId, e);
        }


        PinManagerExtendLifetimeMessage extendLifetime =
            new PinManagerExtendLifetimeMessage(
            pnfsId, pinId,newPinLifetime);
        extendLifetime.setAuthorizationRecord((AuthorizationRecord) user);

        try {
            CellMessage response =  sendAndWait( new CellMessage(
                    new CellPath( "PinManager") ,
                    extendLifetime ) ,
                    60*60*1000
                    );
            if(response == null) {
                throw new SRMException(
                    "PinManagerExtendLifetimeMessage response lifetime expired");
            }
            extendLifetime =
                (PinManagerExtendLifetimeMessage) response.getMessageObject();
            //say("StageAndPinCompanion: recordAsPinned");
            //rr.recordAsPinned (_fr,true);
        } catch (NoRouteToCellException e) {
            throw new SRMException("PinManager is unavailable: " +
                                   e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new SRMException("Request to PinManager got interrupted", e);
        }

        if(extendLifetime.getReturnCode() != 0) {
            throw new SRMException(
                "extendPinLifetime failed, PinManagerExtendLifetimeMessage.returnCode="+
                extendLifetime.getReturnCode()+
                " errorObject = "+extendLifetime.getErrorObject());
        }
        return extendLifetime.getNewLifetime();
    }

    public String getStorageBackendVersion() {
        return diskCacheV111.util.Version.getVersion();
    }

    public boolean exists(SRMUser user,
                          String path)  throws SRMException {
            String absolute_path = srm_root + "/" + path;
            try {
                return _pnfs.getPnfsIdByPath(absolute_path) != null;
            }
            catch (CacheException e) {
                if (e.getRc() == CacheException.FILE_NOT_FOUND ||
                    e.getRc() == CacheException.NOT_IN_TRASH) {
                    return false;
                }
                _log.warn("Failed to find file by path : "+e.getMessage());
                throw new SRMException("Failed to find file by path due to internal system failure or timeout : "+e.getMessage());
            }
            catch (RuntimeException e) {
                _log.error("Unexpected Exception ",e);
                throw new SRMException("Failed to find file by path due to internal system failure or timeout, unexpected exception thrown : "+e.getMessage());
            }
    }

    /**
     * Given a path relative to the root path, this method returns a
     * full PNFS path.
     */
    private String getFullPath(String path)
    {
        FsPath fullPath = new FsPath(srm_root + "/" + path);
        return fullPath.toString();
    }
}
