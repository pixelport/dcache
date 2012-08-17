// $Id: WebPicturesV0.java,v 1.5 2005-07-19 11:02:10 patrick Exp $Cg

package  diskCacheV111.services.web ;

import java.util.* ;
import java.text.* ;
import java.io.* ;

import java.awt.* ;
import java.awt.image.* ;
import javax.imageio.* ;
import javax.imageio.stream.*;

import dmg.cells.nucleus.* ;
import dmg.util.* ;
import dmg.cells.network.CellDomainNode ;
import dmg.cells.nucleus.CellTunnelInfo ;
import dmg.cells.nucleus.CellDomainInfo ;
import dmg.cells.nucleus.CellInfo ;
import diskCacheV111.vehicles.RestoreHandlerInfo ;


import diskCacheV111.poolManager.* ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  *  @Author: Patrick Fuhrmann
  *
  */
public class WebPicturesV0 extends CellAdapter implements Runnable {

   private final static Logger _log =
       LoggerFactory.getLogger(WebPicturesV0.class);

   private CellNucleus      _nucleus   = null ;
   private Args             _args      = null ;
   private Date             _started   = null ;
   private long             _sleep     = (long)(5 * 60 * 1000 ) ;
   private final Object           _sleeper   = new Object() ;

   private boolean          _simulation   = false ;
   private Point            _actionPoint  = null ;
   private int              _binCount     =  40 ;
   private Dimension        _dimension    = new Dimension(400,300);
   private Map<String,Object> _cellContext;
   private boolean          _wasStarted   = false ;
   private Date       _lastMessageArrived = null ;
   private SimpleDateFormat _simpleFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

   private RestoreHandlerInfo [] _currentInfo = null ;

   public WebPicturesV0( String name , String args )throws Exception {

      super( name , args , false ) ;
      _log.info("WebPictures started");
      try{
         _args        = getArgs() ;
         _nucleus     = getNucleus() ;
         _started     = new Date() ;

         _cellContext = _nucleus.getDomainContext() ;

         System.setProperty( "java.awt.headless" , "true");
         /*
          *
          *    -imageSize=<xSize>:<ySize>
          *    -inteval=<seconds>
          *    -archive=<archiveDirectory>
          */

         String sizeRange = _args.getOpt("imageSize") ;
         if( sizeRange != null ){
            try{
               int ind = sizeRange.indexOf(":") ;
               if( ( ind <= 0 ) || ( ind == ( sizeRange.length() - 1 ) ) )
                 throw new
                 IllegalArgumentException("Not a size pair");

               int low  = Integer.parseInt( sizeRange.substring(0,ind) ) ;
               int high = Integer.parseInt( sizeRange.substring(ind+1) ) ;

               _dimension = new Dimension( low , high ) ;
            }catch(NumberFormatException ee ){
                _log.warn("Invalid size string (command ignored) : "+sizeRange ) ;
            }
         }
         _log.info("Image size : "+_dimension);
         String intervalString = _args.getOpt("interval") ;
         if( intervalString != null )
         try{
             _sleep = 1000L * (long)Integer.parseInt(intervalString)  ;
         }catch(NumberFormatException ee ){
             _log.warn("Invalid 'interval' string (command ignored) : "+intervalString ) ;
         }
         _log.info("Interval (msec) : "+_sleep);

         if( _args.hasOption("dontstart") ){ // debug only
            _log.info("Worker Thread not started : -dontstart");
         }else{
            _log.info("Starting worker Thread");
            _nucleus.newThread( this , "Worker" ).start() ;
            _wasStarted = true ;
         }

      }catch(Exception ee){
         start();
         kill() ;
         throw ee ;
      }
      start() ;

   }
   public String hh_start = " # start worker thread";
   public String ac_start( Args args ){
      synchronized( this ){
         if( _wasStarted )
            throw new
            IllegalArgumentException("Thread already running");
         _wasStarted = true ;
      }
      _nucleus.newThread( this , "Worker" ).start() ;
      return "Started";

   }
   public void getInfo( PrintWriter pw ){
       pw.println("   Cell Name : "+getCellName());
       pw.println("  Cell Class : "+this.getClass().getName() );
       pw.println("     Version : $Id: WebPicturesV0.java,v 1.5 2005-07-19 11:02:10 patrick Exp $");
       pw.println("     Started : "+_started);
       pw.println("     Sleep   : "+_sleep);
       pw.println("Picture Size : "+_dimension);
       pw.println("    Last Msg : "+_lastMessageArrived);
   }

   public void run(){
       Thread thread = Thread.currentThread() ;
       CellPath path = new CellPath("PoolManager");
       CellMessage msg = new CellMessage( path , "xrc ls");
       while( ! thread.interrupted() ){

           try{

               _log.info("Sending query : "+msg);

               sendMessage( msg ) ;

               createTransferPicture() ;
               createFrame();

               synchronized( _sleeper ){

                   _sleeper.wait(_sleep);

               }

           }catch(InterruptedException ie ){
               _log.warn("Worker interrupted");
               break ;
           }catch(Exception ee ){
               _log.warn("Exception in while loop : "+ee, ee ) ;
               try{
                   synchronized( _sleeper ){

                       _sleeper.wait((long)(60*1000));

                   }
               }catch(InterruptedException iee ){
                   _log.warn("Worker interrupted2");
                   break ;
               }
           }

       }

   }
   public void messageArrived( CellMessage message ){
      Object obj = message.getMessageObject() ;
         if( obj instanceof RestoreHandlerInfo []  ){
             _currentInfo = (RestoreHandlerInfo[])obj ;
             _log.info("RestoreHandlerInfo ["+_currentInfo.length+"]");
             _lastMessageArrived = new Date() ;
             createRestorePictures();
             createFrame();
          }else{
             _log.warn("Unknown message type arrived : "+obj.getClass().getName());
         }
   }
   public String hh_go = " # start data collection" ;
   public String ac_go( Args args )throws Exception {
       synchronized( _sleeper ){
           _sleeper.notifyAll() ;
       }
       return "";
   }
   private void createFrame(){
      StringBuffer sb = new StringBuffer();
      sb.append("<html>\n").
         append("<head><title>dCache Queue Histograms</title></head>\n").
         append("<body bgcolor=black>\n");

      String [] histograms = { "RestoreQueueHistogram" ,
                               "TransferHistogram0" ,
                               "TransferHistogram1" ,
                               "TransferHistogram2" ,
                               "TransferHistogram3" ,
                              } ;

      for( int i = 0 ; i < histograms.length ; i ++ ){
         String name = histograms[i] ;
         sb.append("<br><br><br><hr><br><br><br>");
         String date = (String)_cellContext.get(name+".date") ;
         Object pict = _cellContext.get(name+".png") ;
         if( ( date != null ) && ( pict != null ) ){
            sb.append("<center>\n").
               append("<h2><font color=white>").
               append(name).append(" ").append(date).append("</font></h2>\n").
               append("<img src=\"/pictures/").append(name).append(".png\">").
               append("</center>");
         }else{
            sb.append("<center>\n").
               append("<h3><font color=white>").append(name).append(" not yet ready</font></h3>\n").
               append("</center>");
         }

      }
      sb.append("<br><br><br><hr><address><font color=white>&copy; dCache.ORG ; last updated : ").
         append((new Date()).toString()).append("</font></address>\n");

      sb.append("</body>\n</html>\n");


      _cellContext.put( "QueueHistograms.html" , sb.toString() ) ;
   }
   private void createTransferPicture(){
       try{

           java.util.List list = scanTransferTable();

           createTransferPicture( list , "TransferHistogram0" , 0L , _dimension ) ;
           createTransferPicture( list , "TransferHistogram1" , 7L * 24L * 3600L * 1000L , _dimension ) ;
           createTransferPicture( list , "TransferHistogram2" ,      24L * 3600L * 1000L , _dimension ) ;
           createTransferPicture( list , "TransferHistogram3" ,            3600L * 1000L , _dimension ) ;

       }catch(IOException ee){
           _log.warn("Exception in scanTransferTable : "+ee, ee);
          return ;
       }

   }
   private void createTransferPicture( java.util.List list , String name , long maxSize , Dimension dimension ){

       BufferedImage image = new BufferedImage( _dimension.width , _dimension.height , BufferedImage.TYPE_BYTE_INDEXED ) ;

       Graphics graphics = image.getGraphics() ;
       ByteArrayOutputStream outStream = new ByteArrayOutputStream() ;

       try{

           Histogram histogram = prepareTransferHistogram( list , _binCount  , maxSize );

           paintComponent( graphics , dimension , histogram , "Transfers "+_simpleFormat.format(new Date())) ;


          ImageIO.write( image , "png",  outStream );
          outStream.flush();
          outStream.close();
       }catch(IOException ee){
           _log.warn("Exception in writing createTransferPictures : "+ee, ee);
          return ;
       }
       _cellContext.put( name+".png" , outStream.toByteArray() ) ;
       _cellContext.put( name+".date" , ( new Date() ).toString() ) ;

   }
   private void createRestorePictures(){

       BufferedImage image = new BufferedImage( _dimension.width , _dimension.height , BufferedImage.TYPE_BYTE_INDEXED ) ;

       Graphics graphics = image.getGraphics() ;

       RestoreHandlerInfo [] info = _currentInfo ;
       if( info == null ){
           _log.warn("Histogram not yet ready");
       }else{
           Histogram histogram = prepareRestoreManagerHistogram( info , _binCount );
           paintComponent( graphics , _dimension , histogram , "Restore "+_simpleFormat.format(new Date())) ;
       }
       ByteArrayOutputStream outStream = new ByteArrayOutputStream() ;

       try{
          ImageIO.write( image , "png",  outStream );
          outStream.flush();
          outStream.close();
       }catch(IOException ee){
           _log.warn("Exception in writing createRestorePictures : "+ee, ee);
          return ;
       }
       _cellContext.put( "RestoreQueueHistogram.png" , outStream.toByteArray() ) ;
       _cellContext.put( "RestoreQueueHistogram.date" , ( new Date() ).toString() ) ;

   }
   static private class Histogram {
      private int [] _displayErray    = null ;
      private int [] _displayArray    = null ;
      private int    _maxDisplayArray = 0 ;
      private long     _secondsPerMasterBin = 0 ;
      private long     _secondsPerBin       = 0 ;
      private BinScale _masterBin = null ;
      private BinScale _bin       = null ;
   }
   static private  BinScale [] _binDefinition = {
      new BinScale(                10 , 10 , "s"  ) ,
      new BinScale(                60 ,  1 , "m"  ) ,
      new BinScale(               120 ,  2 , "m"  ) ,
      new BinScale(               240 ,  4 , "m"  ) ,
      new BinScale(               300 ,  5 , "m"  ) ,
      new BinScale(               600 , 10 , "m"  ) ,
      new BinScale(              1800 , 30 , "m"  ) ,
      new BinScale(              3600 ,  1 , "h"  ) ,
      new BinScale(          2 * 3600 ,  2 , "h"  ) ,
      new BinScale(          4 * 3600 ,  4 , "h"  ) ,
      new BinScale(          5 * 3600 ,  5 , "h"  ) ,
      new BinScale(         12 * 3600 , 12 , "h"  ) ,
      new BinScale(         24 * 3600 ,  1 , "d"  ) ,
      new BinScale(     2 * 24 * 3600 ,  2 , "d"  ) ,
      new BinScale(     4 * 24 * 3600 ,  4 , "d"  ) ,
      new BinScale(     7 * 24 * 3600 ,  1 , "w"  ) ,
      new BinScale( 2 * 7 * 24 * 3600 ,  2 , "w"  ) ,
   } ;
   private static class BinScale {
      private long   secondsPerBin = 0 ;
      private int    unitCount     = 0 ;
      private String unitName      = null ;
      private BinScale( long secondsPerBin , int unitCount , String unitName ){
         this.secondsPerBin = secondsPerBin ;
         this.unitCount     = unitCount ;
         this.unitName      = unitName ;
      }
      public String toString(){
        return "BinScale("+secondsPerBin+"="+unitCount+" "+unitName+")" ;
      }
   }
   private java.util.List scanTransferTable() throws IOException {

      String transferTable = (String)_cellContext.get("transfers.txt") ;
      if( transferTable == null )
         throw new
         NoSuchElementException( "transfers.txt not found" ) ;

      BufferedReader br = new BufferedReader( new StringReader( transferTable ) ) ;

      return scanTransferTable( br ) ;
   }
   static private java.util.List scanTransferTable( BufferedReader br ) throws IOException {


      ArrayList list = new ArrayList() ;
      String line = null ;
      while( ( line = br.readLine() ) != null ){
         StringTokenizer st = new StringTokenizer( line ) ;
         try{
            st.nextToken() ; st.nextToken() ; st.nextToken() ;
            st.nextToken() ; st.nextToken() ; st.nextToken() ;
            st.nextToken() ; st.nextToken() ; st.nextToken() ;
            String status = st.nextToken() ;
            String time   = st.nextToken() ;
            String mode   = st.nextToken() ;
            long [] t = { Long.parseLong( time )  , 0L } ;
            if( status.equals("WaitingForDoorTransferOk") &&
                mode.equals("No-Mover-Found") )t[1] = t[0] ;
            list.add( t ) ;
         }catch(NumberFormatException ii ){
            continue ;
         }
      }
      return list ;
   }
   static private Histogram prepareTransferHistogram( java.util.List list , int binCount , long cut ){

      Histogram histogram = new Histogram() ;

      long maxTime = 0L ;
      for( Iterator it = list.iterator() ; it.hasNext() ; ){
         long [] t = (long [])it.next() ;
         if( ( cut > 0L ) && ( t[0] > cut ) )continue;
         maxTime = Math.max( maxTime , t[0] ) ;
      }
      long secPerBin = maxTime / (long)binCount / 1000L ;
//      _log.info("secPerBin : "+secPerBin);
      int pos = 0 ;
      for( int n = _binDefinition.length ; pos < n ; pos++ ){
         if( _binDefinition[pos].secondsPerBin > secPerBin )break ;
      }
      histogram._bin = _binDefinition[pos] ;
      secPerBin = _binDefinition[pos].secondsPerBin ;
//      _log.info("Seconds per bin (fixed) : "+_binDefinition[pos]);
      int [] array = new int[binCount];
      int [] erray = new int[binCount];
      long largest = secPerBin * binCount ;
      for( Iterator it = list.iterator() ; it.hasNext() ;  ){
         long [] t = (long [])it.next() ;
         if( ( cut > 0L ) && ( t[0] > cut ) )continue;
         long diff = Math.max( t[0] , 0L ) / 1000L;
         pos = (int)((float)diff/(float)largest*(float)(binCount-1)) ;
         pos = Math.min( pos , binCount-1 ) ;
         array[pos]++;
         if( t[1] != 0L )erray[pos]++ ;
      }
      int maxDisplayArray = 0 ;
      for( int i = 0 , n = array.length ; i < n ; i++ ){
         maxDisplayArray = Math.max( maxDisplayArray , array[i] ) ;
      }
      histogram._maxDisplayArray = maxDisplayArray ;
      histogram._displayArray    = array ;
      histogram._displayErray    = erray ;
      int  binsPerMasterBin      = binCount / 4 ;
      long secondsPerMasterBin = binsPerMasterBin * secPerBin ;

//      _log.info("secPerBin : "+secondsPerMasterBin);

      int masterPos = 0 ;
      for( int n = _binDefinition.length ; masterPos < n ; masterPos++ ){
         if( _binDefinition[masterPos].secondsPerBin >= secondsPerMasterBin )break ;
      }
      masterPos = Math.max(masterPos-1,0);
//      _log.info("Seconds per bin (fixed) : "+_binDefinition[masterPos]);
      histogram._secondsPerMasterBin = _binDefinition[masterPos].secondsPerBin ;
      histogram._masterBin           = _binDefinition[masterPos] ;
      histogram._secondsPerBin       = secPerBin ;

      return histogram ;


   }
   private Histogram prepareRestoreManagerHistogram( RestoreHandlerInfo [] info , int binCount){
      return prepareRestoreManagerHistogram(info,binCount,0);
   }
   private Histogram prepareRestoreManagerHistogram( RestoreHandlerInfo [] info ,
                                       int binCount , int unit ){

      Histogram histogram = new Histogram() ;

      long now      = System.currentTimeMillis();
      long youngest = now ;
      for( int i = 0 , n = info.length ; i < n ; i++ ){
         RestoreHandlerInfo rhi = (RestoreHandlerInfo)info[i];
         long start = rhi.getStartTime() ;
         youngest = Math.min( youngest , start ) ;
      }
      long secPerBin = ( now - youngest ) / (long)binCount / 1000L ;
      _log.info("secPerBin : "+secPerBin);
      int pos = 0 ;
      for( int n = _binDefinition.length ; pos < n ; pos++ ){
         if( _binDefinition[pos].secondsPerBin > secPerBin )break ;
      }
      histogram._bin = _binDefinition[pos] ;
      secPerBin = _binDefinition[pos].secondsPerBin ;
      _log.info("Seconds per bin (fixed) : "+_binDefinition[pos]);
      int [] array = new int[binCount];
      int [] erray = new int[binCount];
      long largest = secPerBin * binCount ;
      for( int i = 0 , n = info.length ; i < n ; i++ ){
         RestoreHandlerInfo rhi = (RestoreHandlerInfo)info[i];
         long diff = ( now - rhi.getStartTime() ) / 1000L;
         pos = (int)((float)diff/(float)largest*(float)(binCount-1)) ;
         pos = Math.min( pos , binCount-1 ) ;
         array[pos]++;
         if( rhi.getErrorCode() != 0 )erray[pos]++ ;
      }
      int maxDisplayArray = 0 ;
      for( int i = 0 , n = array.length ; i < n ; i++ ){
         maxDisplayArray = Math.max( maxDisplayArray , array[i] ) ;
      }
      histogram._maxDisplayArray = maxDisplayArray ;
      histogram._displayArray    = array ;
      histogram._displayErray    = erray ;
      int  binsPerMasterBin      = binCount / 4 ;
      long secondsPerMasterBin = binsPerMasterBin * secPerBin ;

      _log.info("secPerBin : "+secondsPerMasterBin);

      int masterPos = 0 ;
      for( int n = _binDefinition.length ; masterPos < n ; masterPos++ ){
         if( _binDefinition[masterPos].secondsPerBin >= secondsPerMasterBin )break ;
      }
      masterPos = Math.max(masterPos-1,0);
      _log.info("Seconds per bin (fixed) : "+_binDefinition[masterPos]);
      histogram._secondsPerMasterBin = _binDefinition[masterPos].secondsPerBin ;
      histogram._masterBin           = _binDefinition[masterPos] ;
      histogram._secondsPerBin       = secPerBin ;

      return histogram ;


   }
   static private int [] _counterDefinition = {
      1 , 2 , 5 , 10 , 20 , 50 , 100 , 200 ,
      500 , 1000 , 2000 , 5000 , 10000
   };
   static public void paintComponent( Graphics gin , Dimension d , Histogram histogram , String title ){

         Graphics2D g = (Graphics2D) gin ;
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

         g.setColor( Color.black ) ;
         g.fillRect( 0 , 0 , d.width - 1 , d.height - 1 ) ;

         if( histogram == null )return ;

	 int fontSize = 12 ;
         g.setFont( new Font( "Serif" , Font.PLAIN|Font.BOLD , fontSize ) );

         FontMetrics fm = g.getFontMetrics() ;
         int fontHeight = fm.getAscent() - fm.getDescent() ;

         int [] erray = histogram._displayErray ;
         int [] array = histogram._displayArray ;
         int maxDisplayArray = histogram._maxDisplayArray ;

         int leftMargin   = 20 ;
         int rightMargin  = 5 ;
         int tickLength   = 4 ;
         int topMargin    = 40 ;
         //
         // find y bin size
         //
         int ybin = maxDisplayArray / 4 ;
         int pos  = 0 ;
         for( int n = _counterDefinition.length ; pos < n ; pos++ ){
            if( _counterDefinition[pos] >= ybin )break ;
         }
         pos = Math.max(0,pos-1);
         ybin = _counterDefinition[pos] ;
         //
         // find max x label pixel's
         //
         int maxXLabelPixel = fm.stringWidth(""+(ybin * 4));
         leftMargin += ( maxXLabelPixel + 2 + 4 ) ;

//         int bottomMargin = 10 ;

         int baseline = d.height - 10 - fontHeight - 2 - 4 ;
         int height   = baseline - topMargin ;
         //
         // the picture
         //
         int pixelsPerBin = ( d.width - leftMargin - rightMargin  ) / array.length - 1 ;
         int x   = leftMargin  ;
         for( int i = 0 , n = array.length ; i < n ; i++ , x += pixelsPerBin ){
            int y = (int)((float)array[i]/(float)maxDisplayArray*(float)height);
            g.setColor( Color.green ) ;
            g.fillRect( x , baseline - y , pixelsPerBin ,  y );
            if( erray[i] != 0 ){
                y = (int)((float)erray[i]/(float)maxDisplayArray*(float)height);
                g.setColor( Color.red ) ;
                g.fillRect( x , baseline - y , pixelsPerBin ,  y );
            }
         }

         //
         //  axis's
         //
         g.setColor( Color.white ) ;
         //
         // x axis
         //
         g.drawLine( leftMargin - tickLength , baseline ,
                     leftMargin + array.length * pixelsPerBin , baseline ) ;
         //
         // y axis
         //
         g.drawLine( leftMargin , baseline + tickLength ,
                     leftMargin , baseline - height - tickLength ) ;

         int pixelsPerMasterBin = (int)(
             (float)histogram._secondsPerMasterBin /
             (float)histogram._secondsPerBin  *
             (float)pixelsPerBin ) ;

//         _log.info("Pixel : perBin : "+pixelsPerBin+
//                            " , perMaster : "+pixelsPerMasterBin);
         //
         // x ticks and labels
         //
         int xoffset = leftMargin + pixelsPerMasterBin ;
         int unitCount = histogram._masterBin.unitCount ;
         for( int i = 0 , n = array.length * pixelsPerBin ;
              xoffset <= n ;
              i++ , xoffset += pixelsPerMasterBin ){
            g.drawLine( xoffset  , baseline-tickLength ,
                        xoffset  , baseline+tickLength ) ;

            String label = ""  + unitCount +
                           " " + histogram._masterBin.unitName ;

            unitCount += histogram._masterBin.unitCount ;
            int stringWidth = fm.stringWidth(label);
            g.drawString( label ,
                          xoffset - stringWidth/2 ,
                          baseline + tickLength + 2 + fm.getAscent() );
         }

         //
         // draw y ticks
         //
         int yoff = ybin ;
         for( int i = 0 ; yoff < maxDisplayArray ; i ++ , yoff += ybin ){
            int y = (int)((float)yoff/(float)maxDisplayArray*(float)height);
            g.drawLine( leftMargin - tickLength , baseline - y ,
                        leftMargin + tickLength , baseline - y ) ;
            String label = ""+yoff ;
            g.drawString( label ,
                          leftMargin - tickLength - 2 - fm.stringWidth(label)  ,
                          baseline - y + fontHeight/2) ;
         }

	 for( int n = topMargin - 15 ; n > 0 ; n -= 5 ){
            g.setFont( new Font( "SanSerif" , Font.BOLD|Font.ITALIC , n ) );
            fm = g.getFontMetrics() ;
	    int length = fm.stringWidth(title) ;
	    if( length > ( d.width - 20 ) )continue ;
            g.drawString( title ,
                	  ( d.width - length ) / 2  ,
                           fm.getAscent() + 5 ) ;
            break ;
         }
   }
   public static void main( String [] args )throws Exception {
      if( args.length < 3 ){
         System.out.println("Usage : <infile> <outfile> <title> <xsize> <ysize>");
	 System.exit(4);
      }
      BufferedReader br = new BufferedReader( new FileReader( args[0] ) ) ;
      Dimension dimension = new Dimension(400,300);
      if( args.length > 3 ){
         dimension = new Dimension( Integer.parseInt(args[3]) , Integer.parseInt(args[4]) ) ;
      }
      java.util.List list = scanTransferTable(br) ;

      Histogram histogram = prepareTransferHistogram( list , 40 , 0L ) ;
       BufferedImage image = new BufferedImage( dimension.width , dimension.height , BufferedImage.TYPE_BYTE_INDEXED ) ;

       Graphics graphics = image.getGraphics() ;
       ByteArrayOutputStream outStream = new ByteArrayOutputStream() ;

       paintComponent( graphics , dimension, histogram , args[2] ) ;


       ImageIO.write( image , "png",  new File( args[1] ) );

   }

}
