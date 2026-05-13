package com.opclient.core

import io.ktor.client.engine.HttpClientEngine

internal expect fun createHttpEngine(): HttpClientEngine
