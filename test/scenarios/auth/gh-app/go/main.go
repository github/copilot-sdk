package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"time"

	copilot "github.com/github/copilot-sdk/go"
)

const (
	deviceCodeURL  = "https://github.com/login/device/code"
	accessTokenURL = "https://github.com/login/oauth/access_token"
	userURL        = "https://api.github.com/user"
)

type deviceCodeResponse struct {
	DeviceCode      string `json:"device_code"`
	UserCode        string `json:"user_code"`
	VerificationURI string `json:"verification_uri"`
	Interval        int    `json:"interval"`
}

type tokenResponse struct {
	AccessToken      string `json:"access_token"`
	Error            string `json:"error"`
	ErrorDescription string `json:"error_description"`
	Interval         int    `json:"interval"`
}

type githubUser struct {
	Login string `json:"login"`
	Name  string `json:"name"`
}

func postJSON(url string, payload any, target any) error {
	body, err := json.Marshal(payload)
	if err != nil {
		return err
	}
	req, err := http.NewRequest(http.MethodPost, url, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		responseBody, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("request failed: %s %s", resp.Status, string(responseBody))
	}
	return json.NewDecoder(resp.Body).Decode(target)
}

func getUser(token string) (*githubUser, error) {
	req, err := http.NewRequest(http.MethodGet, userURL, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("User-Agent", "copilot-sdk-samples-auth-gh-app")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		responseBody, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("github API failed: %s %s", resp.Status, string(responseBody))
	}
	var user githubUser
	if err := json.NewDecoder(resp.Body).Decode(&user); err != nil {
		return nil, err
	}
	return &user, nil
}

func startDeviceFlow(clientID string) (*deviceCodeResponse, error) {
	var resp deviceCodeResponse
	err := postJSON(deviceCodeURL, map[string]any{
		"client_id": clientID,
		"scope":     "read:user",
	}, &resp)
	return &resp, err
}

func pollForToken(clientID, deviceCode string, interval int) (string, error) {
	delaySeconds := interval
	for {
		time.Sleep(time.Duration(delaySeconds) * time.Second)
		var resp tokenResponse
		if err := postJSON(accessTokenURL, map[string]any{
			"client_id":   clientID,
			"device_code": deviceCode,
			"grant_type":  "urn:ietf:params:oauth:grant-type:device_code",
		}, &resp); err != nil {
			return "", err
		}
		if resp.AccessToken != "" {
			return resp.AccessToken, nil
		}
		if resp.Error == "authorization_pending" {
			continue
		}
		if resp.Error == "slow_down" {
			if resp.Interval > 0 {
				delaySeconds = resp.Interval
			} else {
				delaySeconds += 5
			}
			continue
		}
		if resp.ErrorDescription != "" {
			return "", fmt.Errorf(resp.ErrorDescription)
		}
		if resp.Error != "" {
			return "", fmt.Errorf(resp.Error)
		}
		return "", fmt.Errorf("OAuth polling failed")
	}
}

func main() {
	clientID := os.Getenv("GITHUB_OAUTH_CLIENT_ID")
	if clientID == "" {
		log.Fatal("Missing GITHUB_OAUTH_CLIENT_ID")
	}

	fmt.Println("Starting GitHub OAuth device flow...")
	device, err := startDeviceFlow(clientID)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("Open %s and enter code: %s\n", device.VerificationURI, device.UserCode)
	fmt.Print("Press Enter after you authorize this app...")
	fmt.Scanln()

	token, err := pollForToken(clientID, device.DeviceCode, device.Interval)
	if err != nil {
		log.Fatal(err)
	}

	user, err := getUser(token)
	if err != nil {
		log.Fatal(err)
	}
	if user.Name != "" {
		fmt.Printf("Authenticated as: %s (%s)\n", user.Login, user.Name)
	} else {
		fmt.Printf("Authenticated as: %s\n", user.Login)
	}

	client := copilot.NewClient(&copilot.ClientOptions{
		GitHubToken: token,
	})

	ctx := context.Background()
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Model: "claude-haiku-4.5",
	})
	if err != nil {
		log.Fatal(err)
	}
	defer session.Destroy()

	response, err := session.SendAndWait(ctx, copilot.MessageOptions{
		Prompt: "What is the capital of France?",
	})
	if err != nil {
		log.Fatal(err)
	}
	if response != nil && response.Data.Content != nil {
		fmt.Println(*response.Data.Content)
	}
}
