// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"context"
	"encoding/json"
	"net"
	"reflect"
	"slices"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func TestSkillsSourceRequestArraysOnWire(t *testing.T) {
	for _, test := range []struct {
		name   string
		params any
		want   string
	}{
		{
			name: "install nil",
			params: &SkillsInstallSourceRequest{
				Cwd: "/work", PersonalSkillsDir: "/personal", Project: true,
				ProjectSkillsDir: "/project", Source: "https://example.com/skill.md", UserAgent: "test-agent",
			},
			want: `{"cwd":"/work","personalSkillsDir":"/personal","project":true,"projectSkillsDir":"/project","skillDirectories":[],"source":"https://example.com/skill.md","userAgent":"test-agent"}`,
		},
		{
			name:   "install empty",
			params: &SkillsInstallSourceRequest{SkillDirectories: []string{}},
			want:   `{"cwd":"","personalSkillsDir":"","project":false,"projectSkillsDir":"","skillDirectories":[],"source":"","userAgent":""}`,
		},
		{
			name:   "install populated",
			params: &SkillsInstallSourceRequest{SkillDirectories: []string{"/extra"}},
			want:   `{"cwd":"","personalSkillsDir":"","project":false,"projectSkillsDir":"","skillDirectories":["/extra"],"source":"","userAgent":""}`,
		},
		{
			name:   "remove nil without inventory",
			params: &SkillsRemoveSourceRequest{Cwd: "/work", Target: "/extra"},
			want:   `{"cwd":"/work","skillDirectories":[],"target":"/extra"}`,
		},
		{
			name:   "remove nil inventory lists",
			params: &SkillsRemoveSourceRequest{Cwd: "/work", Target: "skill", Inventory: &SkillsRemoveSourceInventory{ProjectSkillsDir: "/project"}},
			want:   `{"cwd":"/work","inventory":{"personalSkillsDirs":[],"projectSkillsDir":"/project","skills":[]},"skillDirectories":[],"target":"skill"}`,
		},
		{
			name: "remove empty",
			params: &SkillsRemoveSourceRequest{
				SkillDirectories: []string{},
				Inventory:        &SkillsRemoveSourceInventory{PersonalSkillsDirs: []string{}, Skills: []SkillRemoveSourceEntry{}},
			},
			want: `{"cwd":"","inventory":{"personalSkillsDirs":[],"projectSkillsDir":"","skills":[]},"skillDirectories":[],"target":""}`,
		},
		{
			name: "remove populated",
			params: &SkillsRemoveSourceRequest{
				Cwd: "/work", Target: "skill", SkillDirectories: []string{"/extra"},
				Inventory: &SkillsRemoveSourceInventory{
					PersonalSkillsDirs: []string{"/personal"}, ProjectSkillsDir: "/project",
					Skills: []SkillRemoveSourceEntry{{Name: "skill", BaseDir: "/personal/skill", Source: "personal"}},
				},
			},
			want: `{"cwd":"/work","inventory":{"personalSkillsDirs":["/personal"],"projectSkillsDir":"/project","skills":[{"name":"skill","baseDir":"/personal/skill","source":"personal"}]},"skillDirectories":["/extra"],"target":"skill"}`,
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			clientConn, serverConn := net.Pipe()
			defer clientConn.Close()
			defer serverConn.Close()
			client := jsonrpc2.NewClient(clientConn, clientConn)
			server := jsonrpc2.NewClient(serverConn, serverConn)
			requests := make(chan json.RawMessage, 1)
			method := "skills.installSource"
			response := `{"kind":"file","path":"/installed","skillDirectories":[]}`
			var original any
			switch params := test.params.(type) {
			case *SkillsInstallSourceRequest:
				copy := *params
				copy.SkillDirectories = slices.Clone(params.SkillDirectories)
				original = &copy
			case *SkillsRemoveSourceRequest:
				method = "skills.removeSource"
				response = `{"kind":"needsInventory"}`
				copy := *params
				copy.SkillDirectories = slices.Clone(params.SkillDirectories)
				if params.Inventory != nil {
					inventory := *params.Inventory
					inventory.PersonalSkillsDirs = slices.Clone(inventory.PersonalSkillsDirs)
					inventory.Skills = slices.Clone(inventory.Skills)
					copy.Inventory = &inventory
				}
				original = &copy
			}
			server.SetRequestHandler(method, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				requests <- params
				return json.RawMessage(response), nil
			})
			client.Start()
			server.Start()
			defer client.Stop()
			defer server.Stop()
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()
			api := NewServerRPC(client)
			var err error
			switch params := test.params.(type) {
			case *SkillsInstallSourceRequest:
				_, err = api.Skills.InstallSource(ctx, params)
			case *SkillsRemoveSourceRequest:
				_, err = api.Skills.RemoveSource(ctx, params)
			}
			if err != nil {
				t.Fatal(err)
			}
			select {
			case raw := <-requests:
				var got, want any
				if err := json.Unmarshal(raw, &got); err != nil {
					t.Fatal(err)
				}
				if err := json.Unmarshal([]byte(test.want), &want); err != nil {
					t.Fatal(err)
				}
				if !reflect.DeepEqual(got, want) {
					t.Errorf("wire params = %s, want %s", raw, test.want)
				}
			case <-ctx.Done():
				t.Fatal(ctx.Err())
			}
			if !reflect.DeepEqual(test.params, original) {
				t.Errorf("caller options changed: got %#v, want %#v", test.params, original)
			}
		})
	}
}
