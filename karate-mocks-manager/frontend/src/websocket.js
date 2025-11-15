import SockJS from 'sockjs-client'
import { Stomp } from 'stompjs'

let stompClient = null

export const connectWebSocket = (onConnected, onError) => {
  const socket = new SockJS('/ws')
  stompClient = Stomp.over(socket)

  stompClient.connect({}, onConnected, onError)

  return stompClient
}

export const subscribeToRequests = (serverId, callback) => {
  if (stompClient && stompClient.connected) {
    return stompClient.subscribe(`/topic/requests/${serverId}`, (message) => {
      const data = JSON.parse(message.body)
      callback(data)
    })
  }
  return null
}

export const disconnectWebSocket = () => {
  if (stompClient) {
    stompClient.disconnect()
  }
}
