import random

def detect_drowsiness():
    # model = torch.hub.load('yolov5', 'custom', path='last.pt')
    # results = model(frame)
    # if results.xyxy[0].shape[0] > 0:
    #      return "Drowsy"
    # else:
    #     return "Not Drowsy"
    options = ["Drowsy", "Awake"]

    results = random.choice(options)

    return results
