from gtts import gTTS
import pygame
import os
import time

def speak(text):
    tts = gTTS(text, lang='vi')
    filename = "/tmp/voice.mp3"
    tts.save(filename)

    pygame.mixer.init()
    pygame.mixer.music.load(filename)
    pygame.mixer.music.play()

    while pygame.mixer.music.get_busy():
        time.sleep(0.5)

    pygame.mixer.quit()
    os.remove(filename)
