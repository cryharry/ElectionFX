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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class ElectionFXController implements Initializable {
	@FXML
	private Stage primaryStage;
	@FXML
	private ComboBox<Integer> classSel;
	@FXML
	private TableView<StudentBean> studentTable;
	@FXML
	private TableColumn<StudentBean, Integer> classCol, banCol, numCol;
	@FXML
	private TableColumn<StudentBean, String> nameCol;
	@FXML
	private Button searchButton, elecControl, elecResult;
	@FXML
	private TextField banTxt;
	@FXML
	private Alert alert;
	@FXML
	private Label stLabel;
	@FXML
	private TabPane elecTabPane, elecPane;
	@FXML
	private Tab tabA, tabB, elecControlTab, elecResultTab;
	@FXML
	private GridPane tabAGrid, tabBGrid, elecControlGrid, elecResultGrid;
	@FXML
	private ImageView st_image;
	
	int classInt, banInt;
	ResultSet rs;
	ObservableList<StudentBean> studentList = FXCollections.observableArrayList();
	StudentBean stBean;
	DBQue dbQue = new DBQue();
	TableRow<StudentBean> row;
	int i = 0, j = 0, x = 0, y = 0;
	String sql = "";
	ByteArrayOutputStream out;
	Boolean check = false;
		
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		ObservableList<Integer> classCom = FXCollections.observableArrayList(1,2,3);
		classSel.setItems(classCom);
		dragDetectHandle();
		sql = "SELECT st_id, class, ban, num, name, sort, e_sel FROM Election_Cand";
		try {
			rs = dbQue.getRS(sql);
			while(rs.next()) {
				stBean = new StudentBean();
				stBean.setSt_id(new SimpleStringProperty(rs.getString("st_id")));
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
					AddGridItem(tabAGrid,i,j,stBean, "A", check);
					st_image.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
						tabAGrid.getChildren().remove(x);
						try {
							sql = "DELETE FROM Election_CAND WHERE e_sel = 'A' and sort = '"+x+"'";
							dbQue.deleteDB(sql);
						} catch (SQLException e) {
							e.printStackTrace();
						}
						x--;
					});
				} else {
					AddGridItem(tabBGrid,i,j,stBean, "B", check);
					st_image.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
						tabBGrid.getChildren().remove(y);
						try {
							sql = "DELETE FROM Election_CAND WHERE e_sel = 'B' and sort = '"+y+"'";
							dbQue.deleteDB(sql);
						} catch (SQLException e) {
							e.printStackTrace();
						}
						y--;
					});
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void dragDetectHandle() {
		studentTable.setRowFactory(tv -> {
			row = new TableRow<StudentBean>();
			row.setOnDragDetected(event -> {
				if(!row.isEmpty()) {
					stBean = studentTable.getSelectionModel().getSelectedItem();
					Dragboard db = row.startDragAndDrop(TransferMode.COPY);
					db.setDragView(row.snapshot(null, null));
					ClipboardContent cc = new ClipboardContent();
					cc.putString(stBean.getSt_id().get());
					db.setContent(cc);
					LocalDragboard.getInstance().putValue(StudentBean.class, stBean);
					event.consume();
				}
			});
			return row;
		});
			elecTabPane.setOnDragOver(event -> {
				Dragboard db = event.getDragboard();
				if(db.hasString()) {
					event.acceptTransferModes(TransferMode.ANY);
					event.consume();
				}
			});
			elecTabPane.setOnDragDropped(event -> {
				Dragboard db = event.getDragboard();
				if(db.hasString()) {
					/**********드래그 드롭 이부분 수정***************/
					LocalDragboard ldb = LocalDragboard.getInstance();
					StudentBean student = ldb.getValue(StudentBean.class);
					getFTPImage(student.getSt_id().getValue());
					Tab selectTab = elecTabPane.getSelectionModel().selectedItemProperty().getValue();
					check = true;
					if(selectTab == tabA) {
							SearchSwitch(x);
							AddGridItem(tabAGrid,i,j,student, "A", check);
							int index = studentTable.getSelectionModel().getSelectedIndex();
							studentTable.getItems().remove(index);
							st_image.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
								tabAGrid.getChildren().remove(x);
								try {
									sql = "DELETE FROM Election_CAND WHERE e_sel = 'A' and sort = '"+x+"'";
									dbQue.deleteDB(sql);
								} catch (SQLException e) {
									e.printStackTrace();
								}
								studentTable.getItems().add(index, student);;
								x--;
							});
						} else {
							SearchSwitch(y);
							AddGridItem(tabBGrid,i,j,student, "B", check);
							int index = studentTable.getSelectionModel().getSelectedIndex();
							studentTable.getItems().remove(index);
							st_image.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
								tabBGrid.getChildren().remove(y);
								try {
									sql = "DELETE FROM Election_CAND WHERE e_sel = 'B' and sort = '"+y+"'";
									dbQue.deleteDB(sql);
								} catch (SQLException e) {
									e.printStackTrace();
								}
								studentTable.getItems().add(index, student);;
								y--;
							});
						}
					/************************************************/
					event.setDropCompleted(true);
					event.consume();
				}
			});
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

	private void AddGridItem(GridPane tab, int col, int row, StudentBean student, String e_sel, Boolean check) {
		String sql = "";
		VBox vBox = new VBox();
		vBox.getChildren().add(st_image);
		String st_id = student.getSt_id().getValue();
		Integer st_class = student.getSt_class().getValue();
		Integer st_ban = student.getSt_ban().getValue();
		Integer st_num = student.getSt_num().getValue();
		String st_name = student.getSt_name().getValue();
		if(tab==tabAGrid) {
			x++;
			vBox.getChildren().add(new Label("기호 ["+x+"]번 "+st_class+"학년 "+st_ban+"반 "+st_num+"번 "+st_name));
			if(check) {
				sql = "INSERT INTO Election_CAND (e_name, st_id, class, ban, num, name, sort, e_sel) values "
						+ "('회장선거','"+st_id+"',"+st_class+","+st_ban+","+st_num+",'"+st_name+"',"+x+",'"+e_sel+"')";
			}
		} else {
			y++;
			vBox.getChildren().add(new Label("기호 ["+y+"]번 "+st_class+"학년 "+st_ban+"반 "+st_num+"번 "+st_name));
			if(check) {
				sql = "INSERT INTO Election_CAND (e_name, st_id, class, ban, num, name, sort, e_sel) values "
						+ "('부회장선거','"+st_id+"',"+st_class+","+st_ban+","+st_num+",'"+st_name+"',"+y+",'"+e_sel+"')";
			}
		}
		vBox.setAlignment(Pos.CENTER);
		tab.add(vBox, col, row);
		try {
			dbQue.insertDB(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void searchStudent() {
		Boolean flag = true;
		alert = new Alert(AlertType.INFORMATION);
		if(classSel.getSelectionModel().getSelectedIndex() != -1) {
			classInt = classSel.getSelectionModel().getSelectedIndex()+1;
			if(!banTxt.getText().equals("")) {
				banInt = Integer.valueOf(banTxt.getText());
			}	else {
				flag = false;
				alert.setContentText("반을 입력해주세요");
				alert.showAndWait();
			}
		} else {
			flag = false;
			alert.setContentText("학년을 선택해주세요");
			alert.showAndWait();
		}
		if(flag) {
			String sql = "SELECT st_id, class, ban, num, name FROM STUDENT where class="+classInt+" and ban="+banInt;
			sql += "order by class, ban, num";
			try {
				rs = dbQue.getRS(sql);
				while(rs.next()) {
					stBean = new StudentBean();
					stBean.setSt_id(new SimpleStringProperty(rs.getString("st_id")));
					stBean.setSt_class(new SimpleIntegerProperty(rs.getInt("class")));
					stBean.setSt_ban(new SimpleIntegerProperty(rs.getInt("ban")));
					stBean.setSt_num(new SimpleIntegerProperty(rs.getInt("num")));
					stBean.setSt_name(new SimpleStringProperty(rs.getString("name")));
					studentList.add(stBean);
				}
				classCol.setCellValueFactory(studentList->studentList.getValue().getSt_class().asObject());
				banCol.setCellValueFactory(studentList->studentList.getValue().getSt_ban().asObject());
				numCol.setCellValueFactory(studentList->studentList.getValue().getSt_num().asObject());
				nameCol.setCellValueFactory(studentList->studentList.getValue().getSt_name());
				studentTable.setItems(studentList);
			} catch (SQLException e) {
				alert = new Alert(AlertType.INFORMATION);
				alert.setContentText(e.toString());
				alert.showAndWait();
			}
			dbQue.closeDB();
		}
	}
	
	@FXML
	public void changeFXML() throws IOException {
		Stage stage;
		Parent root;
		stage = (Stage)elecResult.getScene().getWindow();
		root = FXMLLoader.load(getClass().getResource("ElectionFX2.fxml"));
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
}