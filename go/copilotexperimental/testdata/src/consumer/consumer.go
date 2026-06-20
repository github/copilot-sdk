package consumer

import "sdk"

func useStable() {
	_ = sdk.StableGreeting("world")
	client := &sdk.Client{Name: "ok"}
	client.Connect()
}

func useExperimental() {
	_ = sdk.StartCanvas()         // want `experimental API 'StartCanvas'`
	var options sdk.CanvasOptions // want `experimental API 'CanvasOptions'`
	options.Title = "x"
	_ = options

	client := &sdk.Client{}
	client.EnableMCPApps = true     // want `experimental API 'EnableMCPApps'`
	client.EnableExperimentalMode() // want `experimental API 'EnableExperimentalMode'`
}

func optedIn() {
	_ = sdk.StartCanvas() //nolint:copilotexperimental
}
