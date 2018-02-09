package application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ElectionFXController2 implements Initializable {
	@FXML
	private Stage primaryStage;
	@FXML
	private Button elecControl, elecResult;
	@FXML
	private Alert alert;
	@FXML
	private Label stLabel, studentCount, elecCount, elecNoCount, elecPercent;
	@FXML
	private TabPane elecPane;
	@FXML
	private Tab elecControlTab, elecResultTab, noElecTab;
	@FXML
	private GridPane elecControlGrid, elecResultGrid;
	@FXML
	private ImageView st_image;
	@FXML
	private CheckBox add3Check;
	
	ResultSet rs, rs2, rs3;
	ObservableList<StudentBean> studentList = FXCollections.observableArrayList();
	StudentBean stBean;
	DBQue dbQue = new DBQue();
	int i = 0, j = 0;
	String sql = "", sql2 = "";
	ByteArrayOutputStream out;
	int allSt = 0, elecSt = 0, noSt = 0, newAllSt = 0;
	Double elecPer = 0.0;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		sql = "SELECT COUNT(st_id) as allst FROM student WHERE state = 'Y' and class in (1,2)";
		try {
			rs = dbQue.getRS(sql);
			if(rs.next()) {
				allSt = rs.getInt("allst");
				studentCount.setText(String.valueOf(allSt));
			}
			sql = "SELECT COUNT(st_id) as elecSt FROM election_list where e_sel='A'";
			rs = dbQue.getRS(sql);
			if(rs.next()) {
				elecSt = rs.getInt("elecSt");
				elecCount.setText(String.valueOf(elecSt));
			}
			sql = "SELECT COUNT(st_id) as newAllSt FROM student WHERE state = 'Y'"; 
			rs3 = dbQue.getRS(sql);
			noSt = allSt - elecSt;
			elecNoCount.setText(String.valueOf(noSt));
			elecPer = ((double)elecSt / (double)allSt)*100.0;
			double upPer = Math.round(elecPer*100d)/100d;
			elecPercent.setText(upPer + "%");
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		dbQue.closeDB();		
		sql = "SELECT st_id, class, ban, num, name, sort, e_sel FROM Election_Cand";
		
		try {
			rs = dbQue.getRS(sql);
			while(rs.next()) {
				stBean = new StudentBean();
				String st_id = rs.getString("st_id");
				stBean.setSt_id(new SimpleStringProperty(st_id));
				sql2 = "SELECT e_st_id, count(*) as count FROM Election_List WHERE e_st_id = '"+st_id+"' GROUP BY e_st_id";
				rs2 = dbQue.getRS(sql2);
				while(rs2.next()) {
					stBean.setE_st_id(new SimpleStringProperty(rs2.getString("e_st_id")));
					stBean.setCount(new SimpleIntegerProperty(rs2.getInt("count")));
				}
				stBean.setSt_class(new SimpleIntegerProperty(rs.getInt("class")));
				stBean.setSt_ban(new SimpleIntegerProperty(rs.getInt("ban")));
				stBean.setSt_num(new SimpleIntegerProperty(rs.getInt("num")));
				stBean.setSt_name(new SimpleStringProperty(rs.getString("name")));
				stBean.setE_sel(new SimpleStringProperty(rs.getString("e_sel")));
				stBean.setSort(new SimpleIntegerProperty(rs.getInt("sort")));
				studentList.add(stBean);
			}
			
			for(int z=0; z<studentList.size(); z++) {
				String e_sel = studentList.get(z).getE_sel().getValue();
				String st_id =studentList.get(z).getSt_id().getValue();
				Integer sort = studentList.get(z).getSort().getValue()-1;
				getFTPImage(st_id);
				SearchSwitch(sort);
				if(e_sel.equals("A")) {
					AddGridItem(elecControlGrid,i,j,studentList, z, "A");
				} else {
					AddGridItem(elecResultGrid,i,j,studentList, z, "B");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void getFTPImage(String st_id) {
		FTPClient ftp = new FTPClient();
		try {
			ftp.connect(dbQue.getDB().get(0));
			int reply = ftp.getReplyCode();
			
			if(!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				alert.setContentText("ftp 접속 실패");
			} else {
				//ftp.setSoTimeout(10000);
				ftp.login("unicool", "kcm");
				ftp.changeWorkingDirectory("image");
				out = new ByteArrayOutputStream();
				ftp.retrieveFile(st_id+".jpg", out);
				if(out!=null) {
					byte[] data = out.toByteArray();
					ByteArrayInputStream input = new ByteArrayInputStream(data);
					
					BufferedImage image = ImageIO.read(input);
					st_image = new ImageView();
					st_image.setFitWidth(150);
					st_image.setFitHeight(150);
					if(image!= null) {
						st_image.setImage(SwingFXUtils.toFXImage(image,null));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(ftp != null && ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void SearchSwitch(int code) {
		switch (code) {
		case 0:
			i = 0; j = 0;
			break;
		case 1:
			i = 1; j = 0;
			break;
		case 2:
			i = 2; j = 0;
			break;
		case 3:
			i = 0; j = 1;
			break;
		case 4:
			i = 1; j = 1;
			break;
		case 5:
			i = 2; j = 1;
			break;
		case 6:
			i = 0; j = 2;
			break;
		case 7:
			i = 1; j = 2;
			break;
		case 8:
			i = 2; j = 2;
			break;
		default:
			break;
		}
	}

	private void AddGridItem(GridPane tab, int col, int row, ObservableList<StudentBean> studentList, int z, String e_sel) {
		VBox vBox = new VBox();
		vBox.getChildren().add(st_image);
		Integer st_class = studentList.get(z).getSt_class().getValue();
		Integer st_ban = studentList.get(z).getSt_ban().getValue();
		Integer st_num = studentList.get(z).getSt_num().getValue();
		String st_name = studentList.get(z).getSt_name().getValue();
		Integer sort = studentList.get(z).getSort().getValue();
		String st_id = studentList.get(z).getSt_id().getValue();
		String e_st_id = "";
		Integer count = 0;
		if(studentList.get(z).getE_st_id()!=null) {
			e_st_id = studentList.get(z).getE_st_id().getValue();
		}
		if(studentList.get(z).getCount()!=null) {
			count = studentList.get(z).getCount().getValue();
		}
		vBox.setAlignment(Pos.CENTER);
		vBox.getChildren().add(new Label("기호 ["+sort+"]번 "+st_class+"학년 "+st_ban+"반 "+st_num+"번 "+st_name));
		if(st_id.equals(e_st_id)) {
			vBox.getChildren().add(new Label("투표획득수: "+ count));
		}
		tab.add(vBox, col, row);
	}
	
	@FXML
	public void changeFXML() throws IOException{
		Stage stage;
		Parent root;
		stage = (Stage)elecControl.getScene().getWindow();
		root = FXMLLoader.load(getClass().getResource("ElectionFX.fxml"));
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}

	@FXML public void resetResult() throws SQLException, IOException {
		sql = "DELETE FROM Election_List";
		dbQue.deleteDB(sql);
	}
}