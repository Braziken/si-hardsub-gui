package xyz.navinda.sihardsub;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.nio.file.Paths;

public class PrimaryController {
	@FXML private TextField txtVidFile = null;
	@FXML private TextField txtSubFile = null;
	@FXML private TextField txtOutPath = null;
	@FXML private TextField txtSiHardsubPath = null;
	@FXML private TextArea txtOutLog = null;
	@FXML private Label lblStatus = null;
	@FXML private Button btnStop = null;
	@FXML private Button btnStart = null;
	@FXML private ListView<String> listQueue = null;
	private ObservableList<String> queue =FXCollections.observableArrayList ();
	private String videoFile, subFile, outputPath = null;
	private String siHardSubPath = Paths.get(".").toAbsolutePath().normalize().toString() + "/scripts/si_hardsub";
	
	@FXML
	public void initialize() {
		listQueue.setItems(queue);
	}
	
	@FXML
	public void btnSelectVideoClick(Event e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open File");
		File file = chooser.showOpenDialog(null);

		if (file != null) {
			videoFile = file.getAbsolutePath();
			txtVidFile.setText(videoFile);
			outputPath = file.getParent() + "/";
			txtOutPath.setText(outputPath);
		}
		
	}

	@FXML
	public void btnSelectSubClick(Event e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open File");
		chooser.setInitialDirectory(new File(outputPath));
		File file = chooser.showOpenDialog(null);
		if (file != null) {
			subFile = file.getAbsolutePath();
			txtSubFile.setText(subFile);
		}

	}

	@FXML
	public void btnSelectOutPathClick(Event e) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Open File");
		directoryChooser.setInitialDirectory(new File(outputPath));
		File selectedDirectory = directoryChooser.showDialog(null);
		if (selectedDirectory != null) {
			outputPath = selectedDirectory.getAbsolutePath() + "/";
			txtOutPath.setText(outputPath);
		}
	}

	@FXML
	public void btnStartClick(Event e) {
		if (!checkInputs()) {
			showErrorAlert("Some inputs are missing!.");
			return;
		}
		
		if (queue.size() > 0) {
			String queueItem[] = queue.get(0).split(";");
			videoFile = queueItem[0];
			subFile = queueItem[1];
			outputPath = queueItem[2];
			queue.remove(0);
			startFFmpeg();
		}
		
	}

	@FXML
	public void btnSetSiHardsubClick(Event e) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open File");
		File file = chooser.showOpenDialog(null);
		if (file != null) {
			siHardSubPath = file.getAbsolutePath();
			txtSiHardsubPath.setText(siHardSubPath);
		}
	}

	@FXML
	public void btnStopClick(Event e) {
		try {
			ProcessBuilder proc = new ProcessBuilder("killall", "ffmpeg");
			proc.start();
			btnStop.setDisable(true);
			btnStart.setDisable(false);
		} catch (IOException e1) {
			e1.printStackTrace();
			showErrorAlert(e1.toString() + "\n" + e1.getCause());
		}
	}
	
	@FXML
	public void btnAddToQueueClick(Event e) {
		queue.add(videoFile + ";" + subFile + ";" + outputPath);
	}
	
	@FXML
	public void btnRemoveQueueClick(Event e) {
		if (queue.size() > 0) {
			final int selectedIdx = listQueue.getSelectionModel().getSelectedIndex();
			if (selectedIdx > 0) {
				listQueue.getItems().remove(selectedIdx);
			}
		}
	}

	private void startFFmpeg() {
		Thread FFMpeg = new Thread(() -> {
			String[] arguments = new String[] { siHardSubPath, "-i", videoFile, "-s", subFile, "-o", outputPath };
			try {
				Platform.runLater(() -> {
					lblStatus.setText("Running");
					lblStatus.setTextFill(Color.web("#FF0000"));
					btnStop.setDisable(false);
					btnStart.setDisable(true);
				});
				Process proc = new ProcessBuilder(arguments).redirectErrorStream(true).start();

				BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

				String line = "";

				while ((line = reader.readLine()) != null) {
					final String ln = line;
					Platform.runLater(() -> txtOutLog.appendText(ln + "\n"));
				}

			} catch (Exception e1) {
				e1.printStackTrace();
				Platform.runLater(() -> {
					showErrorAlert(e1.toString() + "\n" + e1.getCause());
					btnStart.setDisable(false);
				});
			} finally {
				Platform.runLater(() -> {
					lblStatus.setText("Stopped");
					lblStatus.setTextFill(Color.web("#0000FF"));
					btnStart.setDisable(false);
					checkQueue();
				});
			}
		});

		FFMpeg.start();
	}

	private void checkQueue() {
		if (queue.size() > 0) {
			String queueItem[] = queue.get(0).split(";");
			videoFile = queueItem[0];
			subFile = queueItem[1];
			outputPath = queueItem[2];
			queue.remove(0);
			startFFmpeg();
		}
	}
	
	private void showErrorAlert(String text) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error!");
		alert.setHeaderText("Details");
		TextArea textArea = new TextArea();
		textArea.setText(text);
		alert.getDialogPane().setContent(textArea);
		alert.showAndWait();
	}

	private boolean checkInputs() {
		return ((videoFile != null) && (subFile != null) && (outputPath != null));
	}
}