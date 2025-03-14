package Assign32starter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.JDialog;
import javax.swing.WindowConstants;
import org.json.JSONObject;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status.
 */
public class ClientGui implements Assign32starter.OutputPanel.EventHandlers {
	private JDialog frame;
	private PicturePanel picPanel;
	private OutputPanel outputPanel;
	private Socket sock;
	private ObjectOutputStream os;
	private BufferedReader bufferedReader;

	private String host;
	private int port;
	private boolean connected = false;

	/**
	 * Constructs the Client GUI.
	 */
	public ClientGui(String host, int port) throws IOException {
		this.host = host;
		this.port = port;

		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(500, 500));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Setup the picture panel
		picPanel = new PicturePanel();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.25;
		frame.add(picPanel, c);

		// Setup the input, button, and output area
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.75;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		outputPanel = new OutputPanel();
		outputPanel.addEventHandlers(this);
		frame.add(outputPanel, c);

		picPanel.newGame(1);
		insertImage("img/Colosseum1.png", 0, 0);

		openConnection(); // Establish connection
		sendRequest(new JSONObject().put("type", "start")); // Initial start request
	}

	/**
	 * Shows the GUI.
	 */
	public void show(boolean makeModal) {
		frame.pack();
		frame.setModal(makeModal);
		frame.setVisible(true);
	}

	/**
	 * Starts a new game.
	 */
	public void newGame(int dimension) {
		picPanel.newGame(1);
		outputPanel.appendOutput("Started new game with a " + dimension + "x" + dimension + " board.");
	}

	/**
	 * Inserts an image into the grid at position (col, row).
	 */
	public boolean insertImage(String filename, int row, int col) throws IOException {
		System.out.println("Image insert");
		String error = "";
		try {
			if (picPanel.insertImage(filename, row, col)) {
				outputPanel.appendOutput("Inserting " + filename + " at (" + row + ", " + col + ")");
				return true;
			}
			error = "File(\"" + filename + "\") not found.";
		} catch (PicturePanel.InvalidCoordinateException e) {
			error = e.toString();
		}
		outputPanel.appendOutput(error);
		return false;
	}

	/**
	 * Handles the submit button click event.
	 */
	@Override
	public void submitClicked() {
		try {
			if (!connected) {
				openConnection(); // Ensure the connection is open
			}

			String input = outputPanel.getInputText().trim();
			if (input.isEmpty()) return;

			// Send user input as JSON
			JSONObject request = new JSONObject();
			request.put("type", "user_input");
			request.put("message", input);

			sendRequest(request);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Key listener for the input text box.
	 */
	@Override
	public void inputUpdated(String input) {
		if (input.equals("surprise")) {
			outputPanel.appendOutput("You found me!");
		}
	}

	/**
	 * Opens a connection to the server.
	 */
	private void openConnection() throws UnknownHostException, IOException {
		if (connected) return; // Avoid reconnecting if already connected

		sock = new Socket(host, port);
		os = new ObjectOutputStream(sock.getOutputStream());
		bufferedReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		connected = true;
		System.out.println("Connected to server at " + host + ":" + port);
	}

	/**
	 * Closes the connection to the server.
	 */
	private void closeConnection() {
		try {
			if (os != null) os.close();
			if (bufferedReader != null) bufferedReader.close();
			if (sock != null) sock.close();
			connected = false;
			System.out.println("Connection closed.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends a JSON request to the server.
	 */
	private void sendRequest(JSONObject request) {
		try {
			if (!connected) openConnection();

			os.writeObject(request.toString());
			os.flush();

			// Read response
			String responseStr = bufferedReader.readLine();
			if (responseStr != null) {
				JSONObject response = new JSONObject(responseStr);
				handleServerResponse(response);
			}
		} catch (IOException e) {
			System.err.println("Error sending request: " + e.getMessage());
			closeConnection();
		}
	}

	/**
	 * Handles server responses.
	 */
	private void handleServerResponse(JSONObject response) {
		String type = response.optString("type", "");
		String message = response.optString("message", "No message received.");

		switch (type) {
			case "request_name":
			case "request_age":
			case "menu":
			case "leaderboard":
			case "game_start":
			case "correct_guess":
			case "wrong_guess":
			case "next_hint":
			case "no_more_hints":
			case "quit":
				outputPanel.appendOutput(message);
				break;

			default:
				outputPanel.appendOutput("Unknown response: " + response.toString());
		}
	}

	/**
	 * Main entry point for the client application.
	 */
	public static void main(String[] args) throws IOException {
		try {
			String host = "localhost";
			int port = 9000;

			ClientGui main = new ClientGui(host, port);
			main.show(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
