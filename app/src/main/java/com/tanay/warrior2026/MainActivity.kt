                        modifier = Modifier.size(22.dp))
                },
                label = {
                    Text(item.label, fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = WarriorRed,
                    selectedTextColor   = WarriorRed,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary,
                    indicatorColor      = Color(0xFF1A0000)
                )
            )
        }
    }
}

// ── Confetti ──────────────────────────────────────────────────
@Composable
fun ConfettiOverlay(onDismiss: () -> Unit) {
    val particles = remember {
        List(50) {
            Triple(
                (0..100).random() / 100f,
                (0..100).random() / 100f,
                listOf(VictoryGreen, WarriorRed, Color.White, Gold,
                    Color(0xFF00BFFF), Color(0xFFFF69B4)).random()
            )
        }
    }
    val progress by animateFloatAsState(
        targetValue  = 1f,
        animationSpec = tween(2000),
        label        = "confetti"
    )
    Box(
        modifier = Modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() },
                indication = null) { onDismiss() }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { (xF, yF, color) ->
                drawCircle(
                    color  = color.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                    radius = 7f,
                    center = androidx.compose.ui.geometry.Offset(
                        xF * size.width,
                        (yF + progress * 0.3f) * size.height
                    )
                )
            }
        }
    }
}

// ── Relapse modal ─────────────────────────────────────────────
@Composable
fun RelapseModal(trollMessage: String, onConfess: (String) -> Boolean, onDismiss: () -> Unit) {
    var urlInput by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(25.dp)
                .clip(RoundedCornerShape(28.dp)).background(Color(0xFF0D0D0D))
                .padding(32.dp, 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(trollMessage, fontSize = 20.sp, fontWeight = FontWeight.Black,
                color = Color.White, textAlign = TextAlign.Center, lineHeight = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text("But today is a new chance to rebuild.",
                fontSize = 13.sp, color = TextTertiary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text("PASTE THE URL (OPTIONAL)", fontSize = 10.sp, color = TextTertiary,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = urlInput,
                onValueChange = { urlInput = it; urlError = false },
                placeholder   = { Text("https://example.com", color = Color(0xFF444444), fontSize = 13.sp) },
                isError       = urlError,
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor      = Color.White,
                    unfocusedTextColor    = Color.White,
                    focusedContainerColor   = Color(0xFF111111),
                    unfocusedContainerColor = Color(0xFF111111),
                    focusedBorderColor    = WarriorRed,
                    unfocusedBorderColor  = BorderColor,
                ),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (urlError) {
                Spacer(Modifier.height(4.dp))
                Text("Invalid URL format.", fontSize = 11.sp, color = WarriorRed, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = { val ok = onConfess(urlInput.ifBlank { "unknown" }); if (!ok) urlError = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = VictoryGreen)
            ) {
                Text("CONFESS & RESET", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ── Panic modal ───────────────────────────────────────────────
@Composable
fun PanicModal(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(25.dp)
                .clip(RoundedCornerShape(28.dp)).background(Color(0xFF0D0D0D))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💀", fontSize = 48.sp)
            Spacer(Modifier.height(10.dp))
            Text("EMERGENCY PROTOCOL", fontSize = 15.sp, fontWeight = FontWeight.Black,
                color = WarriorRed, letterSpacing = 2.sp)
            Spacer(Modifier.height(20.dp))
            listOf(
                "DROP AND DO 50 PUSHUPS",
                "COLD SHOWER — 3 MINUTES",
                "CALL SOMEONE RIGHT NOW",
                "GO OUTSIDE — WALK NOW",
                "DRINK A FULL GLASS OF WATER",
                "LOCK YOUR PHONE IN ANOTHER ROOM",
            ).forEachIndexed { i, cmd ->
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(CardBlack)
                    .padding(16.dp, 12.dp)) {
                    Text("${i + 1}. $cmd", fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("DON'T BE A SLAVE TO CHEAP PIXELS.", fontSize = 11.sp, color = TextTertiary,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = WarriorRed)) {
                Text("I AM IN CONTROL", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}