package Assign32starter;
import java.net.*;
import java.util.Base64;
import java.util.Set;
import java.util.Stack;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import java.awt.image.BufferedImage;
import java.io.*;
import org.json.*;


/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems
 */
public class SockServer {
	static Stack<String> imageSource = new Stack<String>();
	private static HashMap<String, Integer> leaderboard = new HashMap<>();
	private static MovieDatabase movieDatabase = new MovieDatabase();
	private static HashMap<String, GameSession> activeGames = new HashMap<>();

	public static void main(String args[]) {
		int port = 9000; // Default port
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			serverSocket.bind(new InetSocketAddress("0.0.0.0", port)); // Bind to IPv4 explicitly
			System.out.println("Server running on port " + port);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Client connected.");

				new Thread(() -> handleClient(clientSocket)).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void handleClient(Socket clientSocket) {
		try (
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())
		) {
			String playerName = null;

			while (true) {
				String requestStr = (String) in.readObject();
				JSONObject request = new JSONObject(requestStr);
				JSONObject response = new JSONObject();

				String type = request.getString("type");

				switch (type) {
					case "hello":
						response.put("type", "request_name");
						response.put("message", "Enter your name:");
						break;

					case "set_name":
						playerName = request.getString("name");
						activeGames.put(playerName, new GameSession(playerName));
						response.put("type", "request_age");
						response.put("message", "Hello " + playerName + "! Enter your age:");
						break;

					case "set_age":
						response.put("type", "menu");
						response.put("options", new JSONArray().put("leaderboard").put("play").put("quit"));
						response.put("message", "Choose an option:");
						break;

					case "leaderboard":
						response.put("type", "leaderboard");
						response.put("data", getLeaderboard());
						break;

					case "play":
						response = activeGames.get(playerName).startGame();
						break;

					case "guess":
						response = activeGames.get(playerName).processGuess(request.getString("guess"));
						break;

					case "skip":
						response = activeGames.get(playerName).skipRound();
						break;

					case "next_hint":
						response = activeGames.get(playerName).getNextHint();
						break;

					case "quit":
						response.put("type", "quit");
						response.put("message", "Goodbye " + playerName + "!");
						break;

					default:
						response.put("type", "error");
						response.put("message", "Invalid request.");
				}

				out.writeObject(response.toString());
				out.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static JSONObject getLeaderboard() {
		JSONObject leaderboardJSON = new JSONObject();
		for (String player : leaderboard.keySet()) {
			leaderboardJSON.put(player, leaderboard.get(player));
		}
		return leaderboardJSON;
	}
}

class GameSession {
	private String playerName;
	private String currentMovie;
	private int currentHintIndex;
	private int score;
	private MovieDatabase movieDatabase = new MovieDatabase();

	public GameSession(String playerName) {
		this.playerName = playerName;
		this.score = 0;
	}

	public JSONObject startGame() {
		JSONObject response = new JSONObject();
		currentMovie = movieDatabase.getRandomMovie();
		currentHintIndex = 0;

		response.put("type", "game_start");
		response.put("hint", movieDatabase.getHint(currentMovie, currentHintIndex));
		response.put("message", "Guess the movie based on the hint!");
		return response;
	}

	public JSONObject processGuess(String guess) {
		JSONObject response = new JSONObject();

		if (guess.equalsIgnoreCase(currentMovie)) {
			score += 10;
			response.put("type", "correct_guess");
			response.put("message", "Correct! You earned 10 points.");
			response.put("score", score);
		} else {
			response.put("type", "wrong_guess");
			response.put("message", "Wrong! Try again.");
		}
		return response;
	}

	public JSONObject skipRound() {
		return startGame();
	}

	public JSONObject getNextHint() {
		JSONObject response = new JSONObject();
		if (currentHintIndex < 3) {
			currentHintIndex++;
			response.put("type", "next_hint");
			response.put("hint", movieDatabase.getHint(currentMovie, currentHintIndex));
		} else {
			response.put("type", "no_more_hints");
			response.put("message", "No more hints available!");
		}
		return response;
	}
}

/**
 * Stores movie names and hints.
 */
class MovieDatabase {
	private HashMap<String, String[]> movies = new HashMap<>();

	public MovieDatabase() {
		movies.put("Titanic", new String[]{"A ship", "An iceberg", "A love story", "1997"});
		movies.put("Inception", new String[]{"Dreams", "Spinning top", "Christopher Nolan", "2010"});
		movies.put("Avatar", new String[]{"Blue aliens", "Pandora", "James Cameron", "2009"});
		movies.put("The Godfather", new String[]{"Mafia", "Don Corleone", "Family", "1972"});
	}

	public String getRandomMovie() {
		Object[] keys = movies.keySet().toArray();
		return (String) keys[new Random().nextInt(keys.length)];
	}

	public String getHint(String movie, int index) {
		return movies.get(movie)[index];
	}
}