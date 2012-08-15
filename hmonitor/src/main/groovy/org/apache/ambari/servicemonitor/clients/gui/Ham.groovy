/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.servicemonitor.clients.gui

import javax.swing.WindowConstants as WC
import javax.swing.BorderFactory as BF
import java.awt.BorderLayout as BL
import javax.swing.text.StyleConstants as SC

import groovy.swing.SwingBuilder
import groovy.util.logging.Commons
import org.apache.ambari.servicemonitor.MonitorKeys
import org.apache.ambari.servicemonitor.clients.DFSSafeMode
import org.apache.ambari.servicemonitor.clients.JTListQueue
import org.apache.ambari.servicemonitor.clients.JTSafeMode
import org.apache.ambari.servicemonitor.clients.LsDir
import org.apache.ambari.servicemonitor.clients.Operation
import org.apache.ambari.servicemonitor.probes.DfsSafeModeProbe
import org.apache.ambari.servicemonitor.probes.JTSafeModeProbe
import org.apache.ambari.servicemonitor.probes.SafeModeCheck
import org.apache.ambari.servicemonitor.reporting.ProbePhase
import org.apache.ambari.servicemonitor.reporting.ProbeStatus
import org.apache.ambari.servicemonitor.reporting.ReportingLoop
import org.apache.ambari.servicemonitor.utils.Exit
import org.apache.ambari.servicemonitor.utils.MonitorUtils
import org.apache.ambari.servicemonitor.utils.ToolPlusImpl
import org.apache.ambari.servicemonitor.utils.ToolRunnerPlus
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

import java.awt.Color
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Font
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenuBar
import javax.swing.JTextPane
import javax.swing.KeyStroke
import javax.swing.UIManager
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.Style

/**
 * Hadoop Availability Monitor.
 * GUI to keep an eye on what is going on. 
 */
@Commons
public final class Ham extends ToolPlusImpl implements MonitorKeys {

  public static final int LOG_W = 900
  public static final int LOG_H = 600
  public static final String TITLE = 'Hadoop Availability Monitor'
  public static final String NORMAL = "normal"
  public static final String MINOR = "minor"
  public static final String SUCCESS = "success"
  public static final String FAILURE = "failure"
  public static final String DEFAULT_FILESYSTEM = "hdfs://localhost:8020"
  public static final String DEFAULT_JT = "localhost:8021"
  public static final int DEFAULT_HDFS_PORT = 8020
  public static final int PROBE_TIMEOUT = 60000
  public static final int REPORTING_UPDATE_INTERVAL = 500
  public static final int PROBE_INTERVAL = 1000
  public static final int PROBE_BOOTSTRAP_TIMEOUT = 0
  public static final String LOG_FONT = Font.SANS_SERIF
  public static final int LOG_FONT_SIZE = 16
  public static final int LABEL_FONT_SIZE = 20
  public static final String HUNG = 'Offline -hung'
  public static final String ONLINE = 'Online'
  public static final String OFFLINE = 'Offline'
  public static final String SAFE_MODE = 'Safe Mode'
  private BaseClientRunner taskInProgress;
  private ReportingLoop nnReporter;
  private ReportingLoop jtReporter;

  private JTextPane textArea
  private JMenuBar mainMenu
  private JFrame frame
  private String filesystem;
  private String jobtracker;
  private Action aExit
  private Action aClear
  private Action aBlockingRun
  private Action aNonBlockingRun
  private Action aStop
  private DefaultStyledDocument scrollingLogDocument
  private Style normalStyle
  private Style successStyle
  private Style failureStyle
  private Action aCopy
  private Style minorStyle
  private JLabel dfsLabel
  private JLabel nnstatus
  private URI fsURI
  private int hdfsPort
  JLabel jtstatus
  JLabel jtLabel
  URI jtURI
  JTSafeModeProbe jtSafeModeStatus
  SafeModeCheck dfsSafeModeProbe
  Action aJTToggleSafeMode
  Action aDFSToggleSafeMode
  Action aJTListQueue

  public static void main(String[] args) {
    try {
      ToolRunnerPlus.run(new Configuration(),
                         new Ham(),
                         args);
    } catch (Exception e) {
      Exit.exitOnException(e);
    }
  }

  @Override
  int run(String[] args) {
    init()
    build()
    go()
    return 0;
  }

  def init() {
    filesystem = conf.get(FileSystem.FS_DEFAULT_NAME_KEY,
                          DEFAULT_FILESYSTEM)
    if ("file:///".equals(filesystem)) {
      filesystem = DEFAULT_FILESYSTEM;
    }
    fsURI = new URI(filesystem)
    hdfsPort = fsURI.port > 0 ? fsURI.port : DEFAULT_HDFS_PORT
    jtURI = null
    try {
      jobtracker = MonitorUtils.extractJobTrackerParameter(conf);
      if (!jobtracker) {
        jobtracker = DEFAULT_JT;
      }
      jtURI = MonitorUtils.getJTURI(jobtracker)
    } catch (IOException ignored) {
      //JT URL is unknown
      jobtracker = "(undefined)"
    }
  }

  /**
   * This starts up all the probes
   */
  def go() {
    queuePrintln(SUCCESS, TITLE + " working with " + filesystem)
    startNNProbes()
    startJTProbes()
  }

  /**
   * Namenode probes
   */
  public void startNNProbes() {
    def probes = []
//    PortProbe portProbe = PortProbe.createPortProbe(cloneConf(),
//                                                    fsURI.host, hdfsPort)
//    probes << portProbe

    DfsSafeModeProbe safeModeProbe = new DfsSafeModeProbe(cloneConf(), true)
    probes << safeModeProbe
    dfsSafeModeProbe = safeModeProbe
    HamProbeWorker pollWorker = new HamProbeWorker(probes, PROBE_INTERVAL, PROBE_BOOTSTRAP_TIMEOUT)
    def reporter = new HamProbeReporter("HDFS",
                                        this.&probeNNStatusUpdate,
                                        this.&pollingNNTimedOut)
    nnReporter = new ReportingLoop("hdfs",
                                   reporter,
                                   pollWorker,
                                   REPORTING_UPDATE_INTERVAL,
                                   PROBE_TIMEOUT);
    nnstatus.text = 'reporting';
    new Thread(nnReporter).start()
  }

  private Configuration cloneConf() {
    new Configuration(getConf())
  }

  /**
   * JT probes
   */
  public void startJTProbes() {
    if (jtURI) {

      def probes = []

      JTSafeModeProbe jtSafeModeProbe = new JTSafeModeProbe(cloneConf())

      probes << jtSafeModeProbe
      jtSafeModeStatus = jtSafeModeProbe


      HamProbeWorker pollWorker = new HamProbeWorker(probes, PROBE_INTERVAL, PROBE_BOOTSTRAP_TIMEOUT)
      def reporter = new HamProbeReporter("jobtracker",
                                          this.&probeJTStatusUpdate,
                                          this.&pollingJTTimedOut)
      def jtreporter = new ReportingLoop("jobtracker",
                                         reporter,
                                         pollWorker,
                                         REPORTING_UPDATE_INTERVAL,
                                         PROBE_TIMEOUT);
      jtstatus.text = 'reporting';
      new Thread(jtreporter).start()

    }
  }

  /**
   * React to any attempt to stop the process by trying to shut down 
   * in an ordered manner.
   */
  private void exit() {
    stopRun()
    nnReporter?.close()
    Exit.exitProcess(true)
  }

  /**
   * Build the entire UI.
   */
  def build() {

    try {
      UIManager.setLookAndFeel(UIManager.systemLookAndFeelClassName)
    } catch (Exception e) {
      log.warn("Ignoring " + e, e)
    }

    SwingBuilder swing = new SwingBuilder()

    aExit = swing.action(
        name: 'Exit',
        closure: this.&onExit,
        mnemonic: 'x'
    )

    aClear = swing.action(
        name: 'Clear',
        closure: this.&onClear,
        mnemonic: 'l'
    )
    aBlockingRun = swing.action(
        name: 'Blocking LS',
        closure: this.&onBlockingRun,
        mnemonic: 'r'
    )
    aNonBlockingRun = swing.action(
        name: 'Non-blocking LS',
        closure: this.&onNonBlockingRun,
        mnemonic: 'n'
    )

    aDFSToggleSafeMode = swing.action(
        name: 'Toggle HDFS Safe Mode',
        closure: this.&onDFSToggleSafeMode,
        )
    aStop = swing.action(
        name: 'Stop',
        closure: this.&onStop,
        mnemonic: 's'
    )
    aJTToggleSafeMode = swing.action(
        name: 'Toggle JT Safe Mode',
        closure: this.&onJTToggleSafeMode,
        )

    aJTListQueue = swing.action(
        name: 'JT List queue',
        closure: this.&onJTListQueue,
        )

    aCopy = swing.action(
        name: 'Copy',
        closure: this.&onCopy,
        mnemonic: 'c'
    )

    dfsLabel = makeLabel(swing, filesystem)
    nnstatus = makeLabel(swing, '')

    jtLabel = makeLabel(swing, jobtracker)
    jtstatus = makeLabel(swing, '')

    scrollingLogDocument = new DefaultStyledDocument()

    mainMenu = swing.menuBar {
      menu('Actions') {
        menuItem(action: aNonBlockingRun)
        menuItem(action: aBlockingRun)
        menuItem(action: aDFSToggleSafeMode)
        menuItem(action: aJTToggleSafeMode)
        menuItem(action: aStop)
        menuItem(action: aClear,
                 accelerator: meta('l'))
        separator()
        menuItem(action: aCopy,
                 accelerator: meta('c'))
        separator()
        menuItem(action: aExit,
                 accelerator: meta('q'))
      }
    }

    textArea = swing.textPane(
        size: new Dimension(LOG_W, LOG_H),
        document: scrollingLogDocument,
        background: Color.WHITE,
        editable: false,
        )


    normalStyle = scrollingLogDocument.addStyle(NORMAL, null)
    SC.setFontFamily(normalStyle, LOG_FONT)
    SC.setFontSize(normalStyle, LOG_FONT_SIZE)
    SC.setForeground(normalStyle, new Color(0, 0, 128))

    minorStyle = scrollingLogDocument.addStyle(MINOR, normalStyle)
    SC.setForeground(minorStyle, Color.GRAY)

    successStyle = scrollingLogDocument.addStyle(SUCCESS, normalStyle)
    SC.setForeground(successStyle, new Color(0, 128, 0))

    failureStyle = scrollingLogDocument.addStyle(FAILURE, normalStyle)
    SC.setForeground(failureStyle, Color.RED)


    frame = swing.frame(
        title: TITLE,
        defaultCloseOperation: WC.EXIT_ON_CLOSE,
        location: new Point(100, 100),
        size: new Dimension(LOG_W + 20, LOG_H)) {
      widget(mainMenu)
      panel(border: BF.createEmptyBorder(6, 6, 6, 6)) {
        borderLayout()
        vbox(constraints: BL.NORTH) {
          vbox() {

            hbox {
              button(action: aBlockingRun)
              button(action: aNonBlockingRun)
              button(action: aJTListQueue)
              button(action: aStop)
              button(action: aClear)
            }
            hbox {
              widget(makeLabel(swing, 'Filesystem:  '))
              widget(dfsLabel)
              hglue()
              label(text: '    ')
              button(action: aDFSToggleSafeMode)
              widget(nnstatus)
            }
            hbox {
              widget(makeLabel(swing, 'Jobtracker:   '))
              widget(jtLabel)
              hglue()
              label(text: '    ')
              button(action: aJTToggleSafeMode)
              widget(jtstatus)
            }

          }

        }
        vbox(constraints: BL.CENTER) {
          scrollPane {
            widget(textArea)
          }
        }
        vbox(constraints: BL.SOUTH) {
        }
      }
    }
    updateActionStates();
    // frame.pack()
    frame.show()

    queue {
      frame.toFront()
      frame.repaint()
    }

  }

  /**
   * create the label for the text areas. This is where any font and coloring would go
   * @param builder builder
   * @param text the text
   * @return the new lable
   */
  private JLabel makeLabel(SwingBuilder builder, String text) {
    JLabel label = builder.label(text: text
    )
    label.background = Color.WHITE
    label.font = new Font(Font.SANS_SERIF, Font.BOLD, LABEL_FONT_SIZE)
    label
  }

  KeyStroke meta(int key) {
    KeyStroke.getKeyStroke(key,
                           Toolkit.defaultToolkit.menuShortcutKeyMask)
  }

  KeyStroke meta(String key) {
    KeyStroke.getKeyStroke(key.toCharacter(),
                           Toolkit.defaultToolkit.menuShortcutKeyMask)
  }

  def queue(Closure closure) {
    EventQueue.invokeLater closure
  }


  def synchronized updateActionStates() {
    boolean active = taskInProgress != null
    aNonBlockingRun.enabled = !active
    aBlockingRun.enabled = !active
    aJTToggleSafeMode.enabled = !active
    aDFSToggleSafeMode.enabled = !active
    aStop.enabled = active
  }


  def action(String name) {
    //if(name) output("action $name")
    title(name)
  }

  /**
   * Change the title of the application
   * @param text the text to add to the actual {@link #TITLE} constant.
   */
  private void title(String text) {
    String space = text ? " - " : ""
    frame.title = "$TITLE$space$text"
  }

  def onClear(ActionEvent event) {
    action ""
    scrollingLogDocument.remove(0, scrollingLogDocument.length)
  }

  def onBlockingRun(ActionEvent event) {
    action 'Blocking Run'
    startLsDir(true)
  }

  def onNonBlockingRun(ActionEvent event) {
    action 'nonblockingRun'
    startLsDir(false)
  }

  def onDFSToggleSafeMode(ActionEvent event) {
    action 'HDFS Tracker Safe Mode'
    startDFSToggleSafeMode()
  }

  def onJTToggleSafeMode(ActionEvent event) {
    action 'Job Tracker Safe Mode'
    startJTToggleSafeMode()
  }

  def onJTListQueue(ActionEvent event) {
    action 'List Job Tracker Queue'
    startJTListQueue()
  }

  def onStop(ActionEvent event) {
    action 'stop'
    stopRun()
  }

  def onCopy(ActionEvent event) {
    textArea.copy();
  }

  def onExit(ActionEvent event) {
    action 'exit'
    exit()
  }

  /**
   * Output some text -this pushes it out as normal style
   * @param text text to display
   */
  def output(text) {
    output(NORMAL, text)
  }

  def output(String style, String text) {
    scrollingLogDocument.insertString(scrollingLogDocument.length,
                                      text + '\n',
                                      scrollingLogDocument.getStyle(style))
  }

  def queuePrintln(String text) {
    queuePrintln(NORMAL, text)
  }

  def queuePrintln(String style, String text) {
    queue {
      output(style, text)
    }
  }

  /**
   * Callback from operation -not in the GUI thread
   * @param runner runner of action
   * @param text text
   */
  void message(BaseClientRunner runner, String text) {
    queuePrintln(text);
  }

  /**
   * Callback from operation -not in the GUI thread
   * @param runner runner of action
   * @param operation operation in progress
   */
  void startedEvent(BaseClientRunner runner, Operation operation) {
    queuePrintln("Started $operation");
  }

  /**
   * Callback from operation -not in the GUI thread
   * @param runner runner of action
   * @param operation operation in progress
   */
  void waitingEvent(BaseClientRunner runner, Operation operation) {
//    queuePrintln("Waiting for $operation");

  }

  /**
   * Callback from operation -not in the GUI thread
   * @param runner runner of action
   * @param operation operation in progress
   */
  void finishedEvent(BaseClientRunner runner, Operation operation) {
    String outcome = operation.succeeded ? SUCCESS : FAILURE
    queuePrintln(outcome, "$operation")
    if (operation.text) {
      queuePrintln(outcome, operation.text)
    }
  }

  /**
   * Callback from operation -not in the GUI thread
   * @param runner runner of action
   */
  synchronized void endListening(BaseClientRunner runner) {
    queue {
      action ""
      ready();
      taskInProgress = null;
      updateActionStates()
    }
  }

  protected void ready() {
    output(SUCCESS, "ready")
  }

  /**
   * Start a new run
   * @param blocking should the run be blocking?
   * @return true if the run started, false if one was already in progress
   */
  private synchronized boolean startLsDir(boolean blocking) {

    BaseClientRunner operationToRun = new BaseClientRunner(this,
                                                           new LsDir(),
                                                           cloneConf(),
                                                           "--filesystem", filesystem,
                                                           "--dir", "/",
                                                           "--sleeptime", "1000",
                                                           "--successlimit", "-1",
                                                           "--faillimit", "-1",
                                                           "--attempts", "-1",
                                                           blocking ? "-b" : "")
    return startOperation(operationToRun)
  }

  private synchronized boolean startJTToggleSafeMode() {

    BaseClientRunner operationToRun = new BaseClientRunner(this,
                                                           new JTSafeMode(),
                                                           cloneConf(),
                                                           "--sleeptime", "1000",
                                                           "--successlimit", "1",
                                                           "--faillimit", "1",
                                                           "--attempts", "1",
                                                           "--toggle")
    return startOperation(operationToRun)
  }

  private synchronized boolean startDFSToggleSafeMode() {

    BaseClientRunner operationToRun = new BaseClientRunner(this,
                                                           new DFSSafeMode(),
                                                           cloneConf(),
                                                           "--sleeptime", "1000",
                                                           "--successlimit", "1",
                                                           "--faillimit", "1",
                                                           "--attempts", "1",
                                                           "--toggle")
    return startOperation(operationToRun)
  }

  private synchronized boolean startJTListQueue() {

    BaseClientRunner operationToRun = new BaseClientRunner(this,
                                                           new JTListQueue(),
                                                           cloneConf(),
                                                           "--sleeptime", "1000",
                                                           "--successlimit", "-1",
                                                           "--faillimit", "-1",
                                                           "--attempts", "-1")
    return startOperation(operationToRun)
  }

  /**
   * Start any operation
   * @param operationToRun
   * @return
   */
  def startOperation(BaseClientRunner operationToRun) {
    if (taskInProgress) {
      return false
    }
    taskInProgress = operationToRun
    new Thread(taskInProgress).start();
    updateActionStates()
    true
  }

  private synchronized void stopRun() {
    taskInProgress?.shouldExit = true
  }

  private Style createStyle(String name, Style parent, Map attrs) {
    Style style = scrollingLogDocument.addStyle(NORMAL, null)
    addStyleAttrs(style, attrs)
    style
  }

  private void addStyleAttrs(Style style, Map attrs) {
    SimpleAttributeSet atset = new SimpleAttributeSet()
    attrs.each { entry ->
      atset.addAttribute(entry.key, entry.value)
    }
    style.addAttributes(atset)
  }


  def updateFilesystemStatus(ProbeStatus probeStatus, boolean timeout) {
    String text;
    ProbePhase phase = probeStatus.probePhase

    switch (phase) {
      case ProbePhase.DEPENDENCY_CHECKING:
        text = "dependency checking"
        break
      case ProbePhase.LIVE:
      case ProbePhase.BOOTSTRAPPING:
        if (timeout) {
          text = HUNG;
        } else if (probeStatus.success) {
          text = inSafeMode(dfsSafeModeProbe) ? SAFE_MODE : ONLINE
        } else {
          text = OFFLINE
        }
        break
      default:
        text = ""
    }
    if (nnstatus.text != text) {
      log.debug("NN Status -> $text")
      nnstatus.text = text
    }
  }


  def updateJobTrackerStatus(ProbeStatus probeStatus, boolean timeout) {
    String text;
    if (timeout) {
      text = HUNG;
    } else if (probeStatus.success) {
      text = inSafeMode(jtSafeModeStatus) ? SAFE_MODE : ONLINE
    } else {
      text = OFFLINE
    }
    if (jtstatus.text != text) {
      log.debug("JT Status -> $text")
      jtstatus.text = text
    }
  }

  boolean inSafeMode(SafeModeCheck check) {
    check ? check.inSafeMode : false
  }

  /**
   * Closure called on a NN status update
   * @param probeStatus the probe that has changed
   */
  void probeNNStatusUpdate(ProbeStatus probeStatus) {
    queue {
      updateFilesystemStatus(probeStatus, false)
    }
  }

  /**
   * Closure called on a NN poll timeout
   * @param probeStatus the last probe that was received. 
   */
  void pollingNNTimedOut(ProbeStatus probeStatus) {
    log.warn("NN poll timed out -last event $probeStatus")
    queue {
      updateFilesystemStatus(probeStatus, true)
    }
  }

  /**
   * Closure called on a JT status update
   * @param probeStatus the probe that has changed
   */
  void probeJTStatusUpdate(ProbeStatus probeStatus) {
    queue {
      updateJobTrackerStatus(probeStatus, false)
    }
  }

  /**
   * Closure called on a JT poll timeout
   * @param probeStatus the last probe that was received. 
   */
  void pollingJTTimedOut(ProbeStatus probeStatus) {
    log.warn("JT poll timed out -last event $probeStatus")
    queue {
      updateJobTrackerStatus(probeStatus, true)
    }
  }
}
