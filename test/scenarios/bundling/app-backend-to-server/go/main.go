package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"strings"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

func cliURL() string {
	if u := os.Getenv("CLI_URL"); u != "" {
		return u
	}
	if u := os.Getenv("COPILOT_CLI_URL"); u != "" {
		return u
	}
	return "localhost:3000"
}

type chatRequest struct {
	Prompt string `json:"prompt"`
}

type chatResponse struct {
	Response string `json:"response,omitempty"`
	Error    string `json:"error,omitempty"`
}

func chatHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	body, err := io.ReadAll(r.Body)
	if err != nil {
		writeJSON(w, http.StatusBadRequest, chatResponse{Error: "Failed to read body"})
		return
	}

	var req chatRequest
	if err := json.Unmarshal(body, &req); err != nil || req.Prompt == "" {
		writeJSON(w, http.StatusBadRequest, chatResponse{Error: "Missing 'prompt' in request body"})
		return
	}

	client := copilot.NewClient(&copilot.ClientOptions{
		CLIUrl: cliURL(),
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		writeJSON(w, http.StatusInternalServerError, chatResponse{Error: err.Error()})
		return
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: "claude-haiku-4.5",
	})
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, chatResponse{Error: err.Error()})
		return
	}
	defer session.Destroy()

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: req.Prompt,
	})
	if err != nil {
		writeJSON(w, http.StatusInternalServerError, chatResponse{Error: err.Error()})
		return
	}

	if response != nil && response.Data.Content != nil {
		writeJSON(w, http.StatusOK, chatResponse{Response: *response.Data.Content})
	} else {
		writeJSON(w, http.StatusBadGateway, chatResponse{Error: "No response content from Copilot CLI"})
	}
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/chat", chatHandler)

	listener, err := net.Listen("tcp", ":"+port)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("Listening on port %s\n", port)

	if os.Getenv("SELF_TEST") == "1" {
		go func() {
			http.Serve(listener, mux)
		}()

		time.Sleep(500 * time.Millisecond)
		url := fmt.Sprintf("http://localhost:%s/chat", port)
		resp, err := http.Post(url, "application/json",
			strings.NewReader(`{"prompt":"What is the capital of France?"}`))
		if err != nil {
			log.Fatal("Self-test error:", err)
		}
		defer resp.Body.Close()

		var result chatResponse
		json.NewDecoder(resp.Body).Decode(&result)
		if result.Response != "" {
			fmt.Println(result.Response)
		} else {
			log.Fatal("Self-test failed:", result.Error)
		}
	} else {
		http.Serve(listener, mux)
	}
}
